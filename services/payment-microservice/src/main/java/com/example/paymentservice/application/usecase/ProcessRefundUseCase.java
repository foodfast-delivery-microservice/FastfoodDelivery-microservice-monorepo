package com.example.paymentservice.application.usecase;

import com.example.paymentservice.domain.exception.InvalidRefundAmountException;
import com.example.paymentservice.domain.exception.PaymentNotFoundException;
import com.example.paymentservice.domain.entities.OutboxEvent;
import com.example.paymentservice.domain.entities.Payment;
import com.example.paymentservice.domain.repository.OutboxEventRepository;
import com.example.paymentservice.domain.repository.PaymentRepository;
import com.example.paymentservice.application.service.EventPayloadSerializer;

import com.example.paymentservice.infrastructure.event.OrderRefundRequestEvent;
import com.example.paymentservice.infrastructure.event.PaymentRefundedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
public class ProcessRefundUseCase {
    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final EventPayloadSerializer eventPayloadSerializer;

    // 8. USE CASE LUÔN PHẢI LÀ TRANSACTIONAL
    @Transactional
    public void execute(OrderRefundRequestEvent event) {
        log.info("[REFUND_PROCESS] Processing refund - orderId: {}, paymentId: {}, refundAmount: {}, reason: {}", 
                event.getOrderId(), event.getPaymentId(), event.getRefundAmount(), event.getReason());

        // 9. TÌM PAYMENT
        Payment payment = paymentRepository.findById(event.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + event.getPaymentId()));

        // Audit log: Log payment info before refund
        log.info("[REFUND_AUDIT] Refund processing started - paymentId: {}, orderId: {}, userId: {}, merchantId: {}, paymentAmount: {}, refundAmount: {}, reason: {}", 
                payment.getId(), payment.getOrderId(), payment.getUserId(), payment.getMerchantId(), payment.getAmount(), event.getRefundAmount(), event.getReason());

        // 10. GỌI DOMAIN LOGIC
        payment.refund(event.getRefundAmount());

        // 11. LƯU NGHIỆP VỤ CHÍNH
        paymentRepository.save(payment);

        // Audit log: Log successful refund
        log.info("[REFUND_AUDIT] Refund processed successfully - paymentId: {}, orderId: {}, userId: {}, merchantId: {}, refundAmount: {}, reason: {}", 
                payment.getId(), payment.getOrderId(), payment.getUserId(), payment.getMerchantId(), payment.getRefundAmount(), event.getReason());

        // 12. TẠO SỰ KIỆN PHẢN HỒI (DTO)
        PaymentRefundedEvent refundedEventPayload = new PaymentRefundedEvent(
                payment.getId(), // paymentId
                payment.getOrderId(), // orderId
                payment.getStatus().toString(), // "REFUNDED"
                event.getReason()
        );

        // 13. LƯU VÀO OUTBOX (THAY VÌ GỌI RABBITMQ)
        String payloadJson = eventPayloadSerializer.serialize(refundedEventPayload);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(payment.getId().toString())
                .type("PAYMENT_REFUNDED")
                .payload(payloadJson)
                .status(com.example.paymentservice.domain.valueobjects.EventStatus.NEW)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}
