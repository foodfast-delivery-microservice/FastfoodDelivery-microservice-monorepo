package com.example.order_service.infrastructure.listener;

import com.example.order_service.application.usecase.UpdateOrderStatusUseCase;
import com.example.order_service.application.dto.UpdateOrderStatusRequest;
import com.example.order_service.infrastructure.config.RabbitMQConfig;
import com.example.order_service.infrastructure.event.DroneAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for DroneAssignedEvent from Drone Service
 * When a drone is assigned to an order, this listener will update order status to DELIVERING
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DroneAssignedListener {

    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @RabbitListener(queues = RabbitMQConfig.DRONE_ASSIGNED_QUEUE)
    public void handleDroneAssigned(DroneAssignedEvent event) {
        try {
            log.info("🚁 Received DRONE_ASSIGNED event for orderId: {}, droneId: {}, missionId: {}",
                    event.getOrderId(), event.getDroneId(), event.getMissionId());

            // Flow chuẩn: PAID → PROCESSING → DELIVERING
            // Nếu order đang ở PAID, chuyển sang PROCESSING trước
            // Nếu order đang ở PROCESSING, chuyển sang DELIVERING
            
            // Update order status to DELIVERING
            // Note: validateStatusTransition sẽ đảm bảo order phải ở PROCESSING trước khi chuyển sang DELIVERING
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status("DELIVERING")
                    .note(String.format("Đơn hàng đang được giao bởi drone %s (Mission #%d). ETA: %d phút",
                            event.getDroneSerialNumber(), event.getMissionId(), 
                            event.getEstimatedDurationMinutes()))
                    .build();

            updateOrderStatusUseCase.execute(event.getOrderId(), request);

            log.info("✅ Order {} status updated to DELIVERING", event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Failed to process DRONE_ASSIGNED event for orderId: {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}

