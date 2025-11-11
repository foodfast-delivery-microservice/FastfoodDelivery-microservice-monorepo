package com.example.payment.infrastructure.lisener;

import com.example.payment.application.usecase.ProcessRefundUseCase;
import com.example.payment.domain.model.IdempotencyKey;
import com.example.payment.domain.repository.IdempotencyKeyRepository;
import com.example.payment.infrastructure.event.OrderRefundRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // 2. CẦN TRANSACTIONAL

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRefundRequestListener {

    private final ProcessRefundUseCase processRefundUseCase;
    private final IdempotencyKeyRepository idempotencyKeyRepository; // 4. INJECT REPO

    @RabbitListener(queues = "order.refund.request.queue")
    @Transactional // 5. BỌC HÀM NÀY TRONG 1 TRANSACTION
    public void handleOrderRefundRequest(OrderRefundRequestEvent event) {

        Long orderIdKey = event.getOrderId();
        // 1. KIỂM TRA BẰNG 'existsByOrderId'
        if (idempotencyKeyRepository.existsByOrderId(orderIdKey)) {
            log.warn("Duplicate refund request received for order: {}. Skipping...", orderIdKey);
            return;
        }
        // 2. LƯU KHÓA (DÙNG CONSTRUCTOR MỚI)
        IdempotencyKey newKey = new IdempotencyKey(
                event.getOrderId(),
                event.getPaymentId()
        );
        idempotencyKeyRepository.save(newKey);
        try {
            // 9. GỌI NGHIỆP VỤ
            log.info("Processing refund for order: {}", event.getOrderId());
            processRefundUseCase.execute(event);

        } catch (IllegalStateException | IllegalArgumentException e) {
            // LỖI NGHIỆP VỤ - KHÔNG RETRY
            log.error("Business error for order {}: {}", orderIdKey, e.getMessage());
            // Lưu vào Dead Letter Queue hoặc error table
            throw new AmqpRejectAndDontRequeueException("Business validation failed", e);

        } catch (Exception e) {
            // LỖI KỸ THUẬT - CÓ THỂ RETRY
            log.error("Technical error for order {}", orderIdKey, e);
            throw e; // RabbitMQ sẽ retry
        }
    }
}