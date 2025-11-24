package com.example.demo.infrastructure.listener;

import com.example.demo.application.usecase.DeductStockUseCase;
import com.example.demo.domain.model.StockDeductionRecord;
import com.example.demo.domain.repository.StockDeductionRecordRepository;
import com.example.demo.infrastructure.config.RabbitMQConfig;
import com.example.demo.infrastructure.event.OrderPaidEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidListener {

    private final DeductStockUseCase deductStockUseCase;
    private final StockDeductionRecordRepository stockDeductionRecordRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_PAID_QUEUE)
    @Transactional
    public void handleOrderPaid(String jsonPayload) {
        OrderPaidEvent event;
        try {
            // Parse JSON string thành OrderPaidEvent object
            event = objectMapper.readValue(jsonPayload, OrderPaidEvent.class);
        } catch (Exception e) {
            log.error("[STOCK_DEDUCTION] Failed to parse OrderPaidEvent JSON: {}", jsonPayload, e);
            throw new AmqpRejectAndDontRequeueException("Failed to parse event payload", e);
        }
        Long orderId = event.getOrderId();
        
        // Idempotency check: nếu đã xử lý order này rồi thì skip
        if (stockDeductionRecordRepository.existsByOrderId(orderId)) {
            log.warn("[STOCK_DEDUCTION] Duplicate OrderPaid event received for order: {}. Skipping...", orderId);
            return;
        }

        try {
            log.info("[STOCK_DEDUCTION] Received OrderPaidEvent - orderId: {}, merchantId: {}, items count: {}", 
                    orderId, event.getMerchantId(), 
                    event.getOrderItems() != null ? event.getOrderItems().size() : 0);

            if (event.getOrderItems() == null || event.getOrderItems().isEmpty()) {
                log.warn("[STOCK_DEDUCTION] OrderPaidEvent has no order items - orderId: {}", orderId);
                return;
            }

            // Loop qua từng order item và deduct stock TRƯỚC
            for (OrderPaidEvent.OrderItemDeduction item : event.getOrderItems()) {
                try {
                    deductStockUseCase.execute(
                            item.getProductId(),
                            item.getQuantity(),
                            item.getMerchantId()
                    );
                    log.info("[STOCK_DEDUCTION] Stock deducted for productId: {}, quantity: {}, merchantId: {}", 
                            item.getProductId(), item.getQuantity(), item.getMerchantId());
                } catch (IllegalStateException | IllegalArgumentException e) {
                    // Business errors - không retry
                    log.error("[STOCK_DEDUCTION] Business error for productId: {}, quantity: {}, merchantId: {} - Error: {}", 
                            item.getProductId(), item.getQuantity(), item.getMerchantId(), e.getMessage(), e);
                    throw new AmqpRejectAndDontRequeueException("Business validation failed: " + e.getMessage(), e);
                } catch (Exception e) {
                    // Technical errors - có thể retry
                    log.error("[STOCK_DEDUCTION] Technical error for productId: {}, quantity: {}, merchantId: {} - Error: {}", 
                            item.getProductId(), item.getQuantity(), item.getMerchantId(), e.getMessage(), e);
                    throw e; // RabbitMQ sẽ retry
                }
            }

            // Save idempotency record SAU KHI tất cả items đã deduct thành công
            StockDeductionRecord record = new StockDeductionRecord(orderId);
            stockDeductionRecordRepository.save(record);
            log.info("[STOCK_DEDUCTION] Saved idempotency record for orderId: {}", orderId);

            log.info("[STOCK_DEDUCTION] All stock deducted successfully for orderId: {}", orderId);

        } catch (AmqpRejectAndDontRequeueException e) {
            // Re-throw business errors
            throw e;
        } catch (Exception e) {
            log.error("[STOCK_DEDUCTION] Failed to process OrderPaidEvent - orderId: {}, error: {}", 
                    orderId, e.getMessage(), e);
            // Throw exception để RabbitMQ retry
            throw e;
        }
    }
}


