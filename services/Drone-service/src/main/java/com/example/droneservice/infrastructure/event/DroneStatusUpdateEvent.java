package com.example.droneservice.infrastructure.event;

import com.example.droneservice.domain.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published every 2 seconds during drone flight
 * Used for real-time tracking
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DroneStatusUpdateEvent {

    private Long missionId;

    private Long orderId;

    private Long droneId;

    private String droneSerialNumber;

    private Double currentLatitude;

    private Double currentLongitude;

    private Integer batteryLevel;

    private Status status;

    private Integer estimatedArrivalMinutes;
}
