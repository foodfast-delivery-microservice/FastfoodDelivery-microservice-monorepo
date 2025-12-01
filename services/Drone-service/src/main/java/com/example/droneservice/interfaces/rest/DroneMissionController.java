package com.example.droneservice.interfaces.rest;

import com.example.droneservice.application.dto.MissionResponse;
import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.DroneMission;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Drone Mission tracking
 */
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class DroneMissionController {

    private final DroneMissionRepository missionRepository;

    /**
     * Get all missions
     */
    @GetMapping
    public ResponseEntity<List<MissionResponse>> getAllMissions() {
        List<MissionResponse> missions = missionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(missions);
    }

    /**
     * Get mission by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MissionResponse> getMissionById(@PathVariable Long id) {
        return missionRepository.findById(id)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get mission by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<MissionResponse> getMissionByOrderId(@PathVariable Long orderId) {
        return missionRepository.findByOrderId(orderId)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get real-time tracking info for a mission
     */
    @GetMapping("/{id}/tracking")
    public ResponseEntity<TrackingResponse> getTrackingInfo(@PathVariable Long id) {
        return missionRepository.findById(id)
                .map(mission -> {
                    Drone drone = mission.getDrone();

                    TrackingResponse tracking = TrackingResponse.builder()
                            .missionId(mission.getId())
                            .orderId(mission.getOrderId())
                            .droneId(drone.getId())
                            .droneSerialNumber(drone.getSerialNumber())
                            .currentLatitude(drone.getCurrentLatitude())
                            .currentLongitude(drone.getCurrentLongitude())
                            .batteryLevel(drone.getBatteryLevel())
                            .status(mission.getStatus().toString())
                            .estimatedArrivalMinutes(calculateETA(mission))
                            .build();

                    return ResponseEntity.ok(tracking);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get tracking info by order ID (convenience endpoint for frontend)
     */
    @GetMapping("/order/{orderId}/tracking")
    public ResponseEntity<TrackingResponse> getTrackingByOrderId(@PathVariable Long orderId) {
        return missionRepository.findByOrderId(orderId)
                .map(mission -> {
                    Drone drone = mission.getDrone();

                    TrackingResponse tracking = TrackingResponse.builder()
                            .missionId(mission.getId())
                            .orderId(mission.getOrderId())
                            .droneId(drone.getId())
                            .droneSerialNumber(drone.getSerialNumber())
                            .currentLatitude(drone.getCurrentLatitude())
                            .currentLongitude(drone.getCurrentLongitude())
                            .batteryLevel(drone.getBatteryLevel())
                            .status(mission.getStatus().toString())
                            .estimatedArrivalMinutes(calculateETA(mission))
                            .build();

                    return ResponseEntity.ok(tracking);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private MissionResponse mapToResponse(DroneMission mission) {
        return MissionResponse.builder()
                .id(mission.getId())
                .droneId(mission.getDrone().getId())
                .droneSerialNumber(mission.getDrone().getSerialNumber())
                .orderId(mission.getOrderId())
                .pickupLatitude(mission.getPickupLatitude())
                .pickupLongitude(mission.getPickupLongitude())
                .deliveryLatitude(mission.getDeliveryLatitude())
                .deliveryLongitude(mission.getDeliveryLongitude())
                .status(mission.getStatus())
                .distanceKm(mission.getDistanceKm())
                .estimatedDurationMinutes(mission.getEstimatedDurationMinutes())
                .startedAt(mission.getStartedAt())
                .completedAt(mission.getCompletedAt())
                .build();
    }

    private Integer calculateETA(DroneMission mission) {
        if (mission.getStartedAt() == null || mission.getEstimatedDurationMinutes() == null) {
            return null;
        }

        var estimatedCompletion = mission.getStartedAt()
                .plusMinutes(mission.getEstimatedDurationMinutes());

        long minutesRemaining = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                estimatedCompletion).toMinutes();

        return Math.max(0, (int) minutesRemaining);
    }

    /**
     * DTO for real-time tracking response
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class TrackingResponse {
        private Long missionId;
        private Long orderId;
        private Long droneId;
        private String droneSerialNumber;
        private Double currentLatitude;
        private Double currentLongitude;
        private Integer batteryLevel;
        private String status;
        private Integer estimatedArrivalMinutes;
    }
}
