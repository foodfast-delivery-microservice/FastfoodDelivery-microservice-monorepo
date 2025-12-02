package com.example.order_service.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event received from Drone Service when a drone is assigned to an order
 * This event triggers order status update to DELIVERING
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DroneAssignedEvent {

    private Long orderId;

    private Long droneId;

    private String droneSerialNumber;

    private Long missionId;

    private LocalDateTime estimatedArrival;

    private Integer estimatedDurationMinutes;
}

