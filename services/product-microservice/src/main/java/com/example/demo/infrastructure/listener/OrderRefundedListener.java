package com.example.demo.infrastructure.listener;

import com.example.demo.application.usecase.RestoreStockUseCase;
import com.example.demo.infrastructure.config.RabbitMQConfig;
import com.example.demo.infrastructure.event.OrderRefundedEvent;
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
public class OrderRefundedListener {

    private final RestoreStockUseCase restoreStockUseCase;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_REFUNDED_QUEUE)
    @Transactional
    public void handleOrderRefunded(String jsonPayload) {
        OrderRefundedEvent event;
        try {
            // Parse JSON string thành OrderRefundedEvent object
            event = objectMapper.readValue(jsonPayload, OrderRefundedEvent.class);
        } catch (Exception e) {
            log.error("[STOCK_RESTORE] Failed to parse OrderRefundedEvent JSON: {}", jsonPayload, e);
            throw new AmqpRejectAndDontRequeueException("Failed to parse event payload", e);
        }
        try {
            log.info("[STOCK_RESTORE] Received OrderRefundedEvent - orderId: {}, merchantId: {}, items count: {}", 
                    event.getOrderId(), event.getMerchantId(), 
                    event.getOrderItems() != null ? event.getOrderItems().size() : 0);

            if (event.getOrderItems() == null || event.getOrderItems().isEmpty()) {
                log.warn("[STOCK_RESTORE] OrderRefundedEvent has no order items - orderId: {}", event.getOrderId());
                return;
            }

            // Loop qua từng order item và restore stock
            for (OrderRefundedEvent.OrderItemRefund item : event.getOrderItems()) {
                try {
                    restoreStockUseCase.execute(
                            item.getProductId(),
                            item.getQuantity(),
                            item.getMerchantId()
                    );
                    log.info("[STOCK_RESTORE] Stock restored for productId: {}, quantity: {}, merchantId: {}", 
                            item.getProductId(), item.getQuantity(), item.getMerchantId());
                } catch (IllegalStateException | IllegalArgumentException e) {
                    // Business errors - không retry
                    log.error("[STOCK_RESTORE] Business error for productId: {}, quantity: {}, merchantId: {} - Error: {}", 
                            item.getProductId(), item.getQuantity(), item.getMerchantId(), e.getMessage(), e);
                    throw new AmqpRejectAndDontRequeueException("Business validation failed: " + e.getMessage(), e);
                } catch (Exception e) {
                    // Technical errors - có thể retry
                    log.error("[STOCK_RESTORE] Failed to restore stock for productId: {}, quantity: {}, merchantId: {} - Error: {}", 
                            item.getProductId(), item.getQuantity(), item.getMerchantId(), e.getMessage(), e);
                    throw e; // RabbitMQ sẽ retry
                }
            }

            log.info("[STOCK_RESTORE] All stock restored successfully for orderId: {}", event.getOrderId());

        } catch (AmqpRejectAndDontRequeueException e) {
            // Re-throw business errors
            throw e;
        } catch (Exception e) {
            log.error("[STOCK_RESTORE] Failed to process OrderRefundedEvent - orderId: {}, error: {}", 
                    event.getOrderId(), e.getMessage(), e);
            // Throw exception để RabbitMQ retry
            throw e;
        }
    }
}


