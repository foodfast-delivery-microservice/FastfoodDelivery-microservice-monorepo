package com.example.paymentservice.application.usecase;

import com.example.paymentservice.domain.exception.PaymentValidationException;
import com.example.paymentservice.domain.valueobjects.EventStatus;
import com.example.paymentservice.domain.entities.OutboxEvent;
import com.example.paymentservice.domain.entities.Payment;
import com.example.paymentservice.domain.repository.OutboxEventRepository;
import com.example.paymentservice.domain.repository.PaymentRepository;
import com.example.paymentservice.application.dto.PaymentFailedEventPayload;
import com.example.paymentservice.application.service.EventPayloadSerializer;
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
    private final EventPayloadSerializer eventPayloadSerializer;

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
                .userId(payment.getUserId())
                .reason(reason)
                .build();

        createOutboxEvent(payment, payload);
        log.info("Marked payment {} as FAILED due to merchant deactivation", payment.getId());
    }

    private void createOutboxEvent(Payment payment, PaymentFailedEventPayload payloadObject) {
        try {
            String payloadJson = eventPayloadSerializer.serialize(payloadObject);

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
            throw e; // Re-throw to ensure transaction rollback
        }
    }
}

