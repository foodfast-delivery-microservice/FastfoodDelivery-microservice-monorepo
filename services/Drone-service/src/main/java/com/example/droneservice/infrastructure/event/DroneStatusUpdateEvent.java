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

    private Integer estimatedArrivalMinutes; // Deprecated: use specific ETA fields below
    // Specific ETA fields for different roles
    private Integer estimatedPickupMinutes; // Time to reach pickup location (for merchant)
    private Integer estimatedDeliveryMinutes; // Time to reach delivery location (for user)
    private Integer estimatedReturnToBaseMinutes; // Time to return to base (for admin)
}
