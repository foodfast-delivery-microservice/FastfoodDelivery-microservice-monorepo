package com.example.payment.application.usecase;

import com.example.payment.domain.model.OutboxEvent;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.OutboxEventRepository;
import com.example.payment.domain.repository.PaymentRepository;

import com.example.payment.infrastructure.event.OrderRefundRequestEvent;
import com.example.payment.infrastructure.event.PaymentRefundedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class ProcessRefundUseCase {
    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper; // 7. INJECT OBJECTMAPPER

    // 8. USE CASE LUÔN PHẢI LÀ TRANSACTIONAL
    @Transactional
    public void execute(OrderRefundRequestEvent event) {
        // 9. TÌM PAYMENT
        Payment payment = paymentRepository.findById(event.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // 10. GỌI DOMAIN LOGIC
        payment.refund(event.getRefundAmount());

        // 11. LƯU NGHIỆP VỤ CHÍNH
        paymentRepository.save(payment);

        // 12. TẠO SỰ KIỆN PHẢN HỒI (DTO)
        PaymentRefundedEvent refundedEventPayload = new PaymentRefundedEvent(
                payment.getId(), // paymentId
                payment.getOrderId(), // orderId
                payment.getStatus().toString(), // "REFUNDED"
                event.getReason()
        );

        // 13. LƯU VÀO OUTBOX (THAY VÌ GỌI RABBITMQ)
        try {
            String payloadJson = objectMapper.writeValueAsString(refundedEventPayload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("PAYMENT")
                    .aggregateId(payment.getId().toString())
                    .type("PAYMENT_REFUNDED") // 14. KEY QUAN TRỌNG
                    .payload(payloadJson)
                    .build();

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {
            // Nếu lỗi serialize -> rollback toàn bộ transaction
            throw new RuntimeException("Failed to serialize refund event payload", e);
        }
    }
}
