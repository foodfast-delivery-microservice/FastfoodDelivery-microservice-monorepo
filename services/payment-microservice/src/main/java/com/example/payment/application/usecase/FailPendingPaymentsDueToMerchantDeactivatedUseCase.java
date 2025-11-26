package com.example.payment.application.usecase;

import com.example.payment.domain.exception.PaymentValidationException;
import com.example.payment.domain.model.EventStatus;
import com.example.payment.domain.model.OutboxEvent;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.OutboxEventRepository;
import com.example.payment.domain.repository.PaymentRepository;
import com.example.payment.infrastructure.event.PaymentFailedEventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FailPendingPaymentsDueToMerchantDeactivatedUseCase {

    private static final String FAIL_REASON = "Merchant đã ngừng hoạt động";

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void execute(Long merchantId) {
        if (merchantId == null || merchantId <= 0) {
            throw new PaymentValidationException("merchantId không hợp lệ");
        }
        List<Payment> pendingPayments = paymentRepository.findByMerchantIdAndStatus(merchantId, Payment.Status.PENDING);
        if (pendingPayments.isEmpty()) {
            log.debug("No pending payments found for inactive merchant {}", merchantId);
            return;
        }

        pendingPayments.forEach(payment -> failPayment(payment, FAIL_REASON));
    }

    private void failPayment(Payment payment, String reason) {
        payment.setStatus(Payment.Status.FAILED);
        payment.setFailReason(reason);
        paymentRepository.save(payment);

        PaymentFailedEventPayload payload = PaymentFailedEventPayload.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .reason(reason)
                .build();

        createOutboxEvent(payment, payload);
        log.info("Marked payment {} as FAILED due to merchant deactivation", payment.getId());
    }

    private void createOutboxEvent(Payment payment, PaymentFailedEventPayload payloadObject) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payloadObject);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Payment")
                    .aggregateId(payment.getId().toString())
                    .type("PAYMENT_FAILED")
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to create outbox event for payment {}", payment.getId(), e);
        }
    }
}

