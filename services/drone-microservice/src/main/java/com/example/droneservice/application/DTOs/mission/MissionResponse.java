package com.example.droneservice.application.DTOs.mission;

import com.example.droneservice.domain.valueobjects.SerialNumber;
import com.example.droneservice.domain.valueobjects.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MissionResponse {
    private Long id;
    private Long droneId;
    private SerialNumber droneSerialNumber;
    private Long orderId;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private Status status;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    /**
     * Convert DroneMission entity to MissionResponse DTO
     */
    public static MissionResponse fromEntity(com.example.droneservice.domain.entities.DroneMission mission) {
        return MissionResponse.builder()
                .id(mission.getId())
                .droneId(mission.getDrone().getId())
                .droneSerialNumber(mission.getDrone().getSerialNumber()) // Extract from SerialNumber
                .orderId(mission.getOrderId())
                .pickupLatitude(mission.getPickupLocation().getLatitude()) // Extract from Coordinates
                .pickupLongitude(mission.getPickupLocation().getLongitude())
                .deliveryLatitude(mission.getDeliveryLocation().getLatitude())
                .deliveryLongitude(mission.getDeliveryLocation().getLongitude())
                .status(mission.getStatus())
                .distanceKm(mission.getDistanceKm())
                .estimatedDurationMinutes(mission.getEstimatedDurationMinutes())
                .startedAt(mission.getStartedAt())
                .completedAt(mission.getCompletedAt())
                .build();
    }
}
