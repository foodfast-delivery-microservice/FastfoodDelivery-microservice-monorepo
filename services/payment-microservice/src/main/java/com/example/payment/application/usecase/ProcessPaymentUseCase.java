package com.example.payment.application.usecase;

import com.example.payment.domain.model.EventStatus;
import com.example.payment.domain.model.OutboxEvent;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.OutboxEventRepository;
import com.example.payment.domain.repository.PaymentRepository;

import com.example.payment.application.dto.PaymentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository; // <-- THÊM
    private final ObjectMapper objectMapper; // <-- THÊM
    // private final PaymentGatewayService paymentGatewayService; // Bỏ qua vì ta giả lập

    @Transactional
    public boolean  execute(PaymentRequest request) {
        log.info("Processing payment for orderId: {}", request.getOrderId());

        if (paymentRepository.findByOrderId(request.getOrderId()).isPresent()) {
            log.warn("Payment for orderId: {} already processed. Skipping.", request.getOrderId());
            return false;
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getGrandTotal())
                .currency(request.getCurrency())
                .status(Payment.Status.PENDING)

                .build();

        payment = paymentRepository.save(payment);

        try {
            // TODO: Giả lập gọi cổng thanh toán
            log.info("Payment gateway processing simulation for orderId: {}", request.getOrderId());
            //Thread.sleep(1000); // Giả lập độ trễ

            // 4. Xử lý thành công
            payment.setStatus(Payment.Status.SUCCESS);
            payment.setTransactionNo("DUMMY_TXN_" + System.currentTimeMillis());
            paymentRepository.save(payment);

            log.info("Payment SUCCESS for orderId: {}", request.getOrderId());

            // 5. TẠO OUTBOX EVENT (để báo lại cho order-service)
            PaymentSuccessEventPayload payload = PaymentSuccessEventPayload.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .build();
            createOutboxEvent(payment, "PAYMENT_SUCCESS", payload); // <-- HOÀN THIỆN
            return true;

        } catch (Exception e) {
            log.error("Payment FAILED for orderId: {}", request.getOrderId(), e);

            // 6. Xử lý thất bại
            payment.setStatus(Payment.Status.FAILED);
            payment.setFailReason(e.getMessage());
            paymentRepository.save(payment);

            // 7. TẠO OUTBOX EVENT (để báo lại cho order-service)
            PaymentFailedEventPayload payload = PaymentFailedEventPayload.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .reason(e.getMessage())
                    .build();
            createOutboxEvent(payment, "PAYMENT_FAILED", payload); // <-- HOÀN THIỆN
            return false;
        }
    }

    // Hàm tạo Outbox Event (y hệt bên order-service)
    private void createOutboxEvent(Payment payment, String eventType, Object payloadObject) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payloadObject);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Payment")
                    .aggregateId(payment.getId().toString())
                    .type(eventType)
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(event);

        } catch (JsonProcessingException e) {
            log.error("CRITICAL: Failed to serialize event payload for paymentId: {}. Transaction will be rolled back.", payment.getId(), e);
            throw new RuntimeException("Failed to create outbox event payload", e);
        }
    }

    // --- DTOs cho event gửi đi ---
    @Data @Builder
    private static class PaymentSuccessEventPayload {
        private Long paymentId;
        private Long orderId;
    }

    @Data @Builder
    private static class PaymentFailedEventPayload {
        private Long paymentId;
        private Long orderId;
        private String reason;
    }
}

