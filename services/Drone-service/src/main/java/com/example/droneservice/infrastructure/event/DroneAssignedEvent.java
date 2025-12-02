package com.example.droneservice.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published to Order Service when drone is assigned
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
