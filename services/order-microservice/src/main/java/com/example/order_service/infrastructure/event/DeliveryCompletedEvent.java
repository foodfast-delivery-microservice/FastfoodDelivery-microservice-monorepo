package com.example.order_service.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event received from Drone Service when delivery is completed
 * This event is published when drone state = DELIVERING and arrives at delivery location
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryCompletedEvent {

    private Long orderId;

    private Long missionId;

    private Long droneId;

    private LocalDateTime completedAt;
}

