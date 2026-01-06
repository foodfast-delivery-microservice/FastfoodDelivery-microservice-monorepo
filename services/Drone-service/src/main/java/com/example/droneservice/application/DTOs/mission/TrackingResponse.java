package com.example.droneservice.application.DTOs.mission;

import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.entities.DroneMission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * DTO for real-time mission tracking response
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrackingResponse {
    private Long missionId;
    private Long orderId;
    private Long droneId;
    private String droneSerialNumber;
    private Double currentLatitude;
    private Double currentLongitude;
    private Double baseLatitude;
    private Double baseLongitude;
    private Integer batteryLevel;
    private String status;
    private Integer estimatedArrivalMinutes;

    /**
     * Convert DroneMission entity to TrackingResponse DTO
     * Includes ETA calculation
     */
    public static TrackingResponse fromEntity(DroneMission mission) {
        Drone drone = mission.getDrone();
        if (drone == null) {
            throw new IllegalStateException("Drone not found for mission " + mission.getId());
        }

        return TrackingResponse.builder()
                .missionId(mission.getId())
                .orderId(mission.getOrderId())
                .droneId(drone.getId())
                .droneSerialNumber(drone.getSerialNumber().getValue()) // Extract from SerialNumber
                .currentLatitude(drone.getCurrentLocation().getLatitude()) // Extract from Coordinates
                .currentLongitude(drone.getCurrentLocation().getLongitude())
                .baseLatitude(drone.getBaseLocation().getLatitude())
                .baseLongitude(drone.getBaseLocation().getLongitude())
                .batteryLevel(drone.getBatteryLevel().getValue()) // Extract from BatteryLevel
                .status(mission.getStatus().toString())
                .estimatedArrivalMinutes(calculateETA(mission))
                .build();
    }

    /**
     * Calculate estimated time of arrival in minutes
     */
    private static Integer calculateETA(DroneMission mission) {
        if (mission.getStartedAt() == null || mission.getEstimatedDurationMinutes() == null) {
            return null;
        }

        LocalDateTime estimatedCompletion = mission.getStartedAt()
                .plusMinutes(mission.getEstimatedDurationMinutes());

        long minutesRemaining = Duration.between(
                LocalDateTime.now(),
                estimatedCompletion).toMinutes();

        return Math.max(0, (int) minutesRemaining);
    }
}
