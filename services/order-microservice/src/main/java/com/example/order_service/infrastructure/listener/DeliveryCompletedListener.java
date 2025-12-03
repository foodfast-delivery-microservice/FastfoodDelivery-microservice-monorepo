package com.example.order_service.infrastructure.listener;

import com.example.order_service.application.usecase.UpdateOrderStatusUseCase;
import com.example.order_service.application.usecase.GetOrderDetailUseCase;
import com.example.order_service.application.dto.UpdateOrderStatusRequest;
import com.example.order_service.infrastructure.config.RabbitMQConfig;
import com.example.order_service.infrastructure.event.DeliveryCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for delivery completed events from Drone Service
 * When drone state = DELIVERING and arrives at delivery location,
 * this listener will update order status to "delivered"
 * 
 * Note: This listener is idempotent - if order is already DELIVERED,
 * it will skip the update to avoid duplicate status change errors.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryCompletedListener {

    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final GetOrderDetailUseCase getOrderDetailUseCase;

    @RabbitListener(queues = RabbitMQConfig.DELIVERY_COMPLETED_QUEUE)
    public void handleDeliveryCompleted(DeliveryCompletedEvent event) {
        try {
            log.info("📦 Received DELIVERY_COMPLETED event for orderId: {}, missionId: {}, droneId: {}",
                    event.getOrderId(), event.getMissionId(), event.getDroneId());

            // Check current order status to avoid duplicate updates (idempotent)
            try {
                var currentOrder = getOrderDetailUseCase.execute(event.getOrderId());
                if ("DELIVERED".equalsIgnoreCase(currentOrder.getStatus())) {
                    log.info("ℹ️ Order {} is already DELIVERED, skipping duplicate update", event.getOrderId());
                    return; // Idempotent: order already in desired state
                }
            } catch (Exception e) {
                log.warn("⚠️ Could not check current order status, proceeding with update: {}", e.getMessage());
                // Continue with update if we can't check status
            }

            // Update order status to "delivered"
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status("DELIVERED")
                    .note("Đơn hàng đã được giao bởi drone #" + event.getDroneId())
                    .build();

            updateOrderStatusUseCase.execute(event.getOrderId(), request);

            log.info("✅ Order {} status updated to DELIVERED", event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Failed to process DELIVERY_COMPLETED event for orderId: {}", 
                    event.getOrderId(), e);
        }
    }
}

