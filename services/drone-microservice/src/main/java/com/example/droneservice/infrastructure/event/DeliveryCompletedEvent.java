package com.example.droneservice.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when delivery is completed
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
