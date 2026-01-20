package com.example.droneservice.infrastructure.listener;

import com.example.droneservice.application.DTOs.drone.AssignDroneRequest;
import com.example.droneservice.application.DTOs.mission.MissionResponse;
import com.example.droneservice.application.usecases.drone.AssignDroneToOrderUseCase;
import com.example.droneservice.infrastructure.config.RabbitMQConfig;
import com.example.droneservice.infrastructure.event.OrderReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for ORDER_READY_TO_SHIP events from Order Service
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReadyListener {

    private final AssignDroneToOrderUseCase assignDroneUseCase;

    @RabbitListener(queues = RabbitMQConfig.ORDER_READY_QUEUE)
    public void handleOrderReadyToShip(OrderReadyEvent event) {
        log.info("📦 Received ORDER_READY_TO_SHIP event for order: {}", event.getOrderId());

        // Validate delivery method
        if (!"DRONE".equalsIgnoreCase(event.getDeliveryMethod())) {
            log.info("⏭️ Order {} is not drone delivery (method: {}), skipping",
                    event.getOrderId(), event.getDeliveryMethod());
            return;
        }

        try {
            // Create assignment request
            AssignDroneRequest request = AssignDroneRequest.builder()
                    .orderId(event.getOrderId())
                    .pickupLatitude(event.getPickupLatitude())
                    .pickupLongitude(event.getPickupLongitude())
                    .deliveryLatitude(event.getDeliveryLatitude())
                    .deliveryLongitude(event.getDeliveryLongitude())
                    .build();

            // Assign drone (this will create OutboxEvent internally)
            MissionResponse mission = assignDroneUseCase.execute(request);

            log.info("✅ Drone {} assigned to order {}. Mission ID: {}, ETA: {} minutes",
                    mission.getDroneSerialNumber(),
                    event.getOrderId(),
                    mission.getId(),
                    mission.getEstimatedDurationMinutes());

            // Note: DRONE_ASSIGNED event is now published via OutboxEventRelay
            // The event is created in AssignDroneToOrderUseCase

        } catch (IllegalStateException e) {
            log.error("❌ No available drones for order {}: {}",
                    event.getOrderId(), e.getMessage());

            // TODO: Publish DRONE_ASSIGNMENT_FAILED event
            // This allows Order Service to fallback to standard delivery
        } catch (Exception e) {
            log.error("❌ Error assigning drone to order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}
