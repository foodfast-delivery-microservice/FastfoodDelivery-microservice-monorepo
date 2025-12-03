package com.example.droneservice.interfaces.rest;

import com.example.droneservice.application.dto.MissionResponse;
import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.DroneMission;
import com.example.droneservice.domain.model.State;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.infrastructure.util.HaversineDistanceCalculator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
     * Get all missions for a specific drone
     */
    @GetMapping("/drone/{droneId}")
    public ResponseEntity<List<MissionResponse>> getMissionsByDroneId(@PathVariable Long droneId) {
        List<MissionResponse> missions = missionRepository.findByDroneId(droneId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(missions);
    }

    /**
     * Get real-time tracking info for a mission
     */
    @GetMapping("/{id}/tracking")
    public ResponseEntity<TrackingResponse> getTrackingInfo(@PathVariable Long id) {
        try {
            return missionRepository.findById(id)
                    .map(mission -> {
                        try {
                            Drone drone = mission.getDrone();
                            if (drone == null) {
                                throw new IllegalStateException("Drone not found for mission " + mission.getId());
                            }

                            TrackingResponse tracking = TrackingResponse.builder()
                                    .missionId(mission.getId())
                                    .orderId(mission.getOrderId())
                                    .droneId(drone.getId())
                                    .droneSerialNumber(drone.getSerialNumber())
                                    .currentLatitude(drone.getCurrentLatitude())
                                    .currentLongitude(drone.getCurrentLongitude())
                                    .baseLatitude(drone.getBaseLatitude())
                                    .baseLongitude(drone.getBaseLongitude())
                                    .batteryLevel(drone.getBatteryLevel())
                                    .status(mission.getStatus().toString())
                                    .estimatedArrivalMinutes(calculateETA(mission)) // Keep for backward compatibility
                                    .estimatedPickupMinutes(calculatePickupETA(mission, drone))
                                    .estimatedDeliveryMinutes(calculateDeliveryETA(mission, drone))
                                    .estimatedReturnToBaseMinutes(calculateReturnToBaseETA(mission, drone))
                                    .build();

                            return ResponseEntity.ok(tracking);
                        } catch (Exception e) {
                            throw new RuntimeException("Error building tracking response: " + e.getMessage(), e);
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get tracking info by order ID (convenience endpoint for frontend)
     */
    @GetMapping("/order/{orderId}/tracking")
    public ResponseEntity<TrackingResponse> getTrackingByOrderId(@PathVariable Long orderId) {
        try {
            return missionRepository.findByOrderId(orderId)
                    .map(mission -> {
                        try {
                            Drone drone = mission.getDrone();
                            if (drone == null) {
                                throw new IllegalStateException("Drone not found for mission " + mission.getId());
                            }

                            TrackingResponse tracking = TrackingResponse.builder()
                                    .missionId(mission.getId())
                                    .orderId(mission.getOrderId())
                                    .droneId(drone.getId())
                                    .droneSerialNumber(drone.getSerialNumber())
                                    .currentLatitude(drone.getCurrentLatitude())
                                    .currentLongitude(drone.getCurrentLongitude())
                                    .baseLatitude(drone.getBaseLatitude())
                                    .baseLongitude(drone.getBaseLongitude())
                                    .batteryLevel(drone.getBatteryLevel())
                                    .status(mission.getStatus().toString())
                                    .estimatedArrivalMinutes(calculateETA(mission)) // Keep for backward compatibility
                                    .estimatedPickupMinutes(calculatePickupETA(mission, drone))
                                    .estimatedDeliveryMinutes(calculateDeliveryETA(mission, drone))
                                    .estimatedReturnToBaseMinutes(calculateReturnToBaseETA(mission, drone))
                                    .build();

                            return ResponseEntity.ok(tracking);
                        } catch (Exception e) {
                            throw new RuntimeException("Error building tracking response: " + e.getMessage(), e);
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
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
     * Calculate ETA to pickup location (for merchant)
     * Returns time in minutes until drone reaches pickup location
     */
    private Integer calculatePickupETA(DroneMission mission, Drone drone) {
        if (drone.getCurrentLatitude() == null || drone.getCurrentLongitude() == null
                || mission.getPickupLatitude() == null || mission.getPickupLongitude() == null) {
            return null;
        }

        State droneState = drone.getState();
        
        // If drone is already at pickup or past pickup, return null
        if (droneState == State.DELIVERING || droneState == State.RETURNING) {
            return null; // Already passed pickup
        }

        // Calculate distance to pickup
        double distanceKm = HaversineDistanceCalculator.calculate(
                drone.getCurrentLatitude(), drone.getCurrentLongitude(),
                mission.getPickupLatitude(), mission.getPickupLongitude());

        // Drone speed: 40 km/h = 0.667 km/min
        double speedKmPerMin = 40.0 / 60.0;
        int minutes = (int) Math.ceil(distanceKm / speedKmPerMin);

        return Math.max(0, minutes);
    }

    /**
     * Calculate ETA to delivery location (for user)
     * Returns time in minutes until drone reaches delivery location
     */
    private Integer calculateDeliveryETA(DroneMission mission, Drone drone) {
        if (drone.getCurrentLatitude() == null || drone.getCurrentLongitude() == null
                || mission.getDeliveryLatitude() == null || mission.getDeliveryLongitude() == null) {
            return null;
        }

        State droneState = drone.getState();
        
        // If drone is returning to base, delivery is already done
        if (droneState == State.RETURNING) {
            return null; // Already delivered
        }

        // Calculate distance to delivery
        double distanceKm = HaversineDistanceCalculator.calculate(
                drone.getCurrentLatitude(), drone.getCurrentLongitude(),
                mission.getDeliveryLatitude(), mission.getDeliveryLongitude());

        // Drone speed: 40 km/h = 0.667 km/min
        double speedKmPerMin = 40.0 / 60.0;
        int minutes = (int) Math.ceil(distanceKm / speedKmPerMin);

        return Math.max(0, minutes);
    }

    /**
     * Calculate ETA to return to base (for admin)
     * Returns time in minutes until drone returns to base
     */
    private Integer calculateReturnToBaseETA(DroneMission mission, Drone drone) {
        if (drone.getCurrentLatitude() == null || drone.getCurrentLongitude() == null
                || drone.getBaseLatitude() == null || drone.getBaseLongitude() == null) {
            return null;
        }

        State droneState = drone.getState();
        
        // If drone is not returning yet, calculate total time from current position
        // This includes: current → delivery (if not delivered) → base
        double distanceKm = 0.0;

        if (droneState == State.RETURNING) {
            // Already delivered, just calculate distance to base
            distanceKm = HaversineDistanceCalculator.calculate(
                    drone.getCurrentLatitude(), drone.getCurrentLongitude(),
                    drone.getBaseLatitude(), drone.getBaseLongitude());
        } else if (droneState == State.DELIVERING) {
            // Still delivering, calculate: current → delivery → base
            double toDelivery = HaversineDistanceCalculator.calculate(
                    drone.getCurrentLatitude(), drone.getCurrentLongitude(),
                    mission.getDeliveryLatitude(), mission.getDeliveryLongitude());
            double deliveryToBase = HaversineDistanceCalculator.calculate(
                    mission.getDeliveryLatitude(), mission.getDeliveryLongitude(),
                    drone.getBaseLatitude(), drone.getBaseLongitude());
            distanceKm = toDelivery + deliveryToBase;
        } else {
            // Going to pickup, calculate: current → pickup → delivery → base
            double toPickup = HaversineDistanceCalculator.calculate(
                    drone.getCurrentLatitude(), drone.getCurrentLongitude(),
                    mission.getPickupLatitude(), mission.getPickupLongitude());
            double pickupToDelivery = HaversineDistanceCalculator.calculate(
                    mission.getPickupLatitude(), mission.getPickupLongitude(),
                    mission.getDeliveryLatitude(), mission.getDeliveryLongitude());
            double deliveryToBase = HaversineDistanceCalculator.calculate(
                    mission.getDeliveryLatitude(), mission.getDeliveryLongitude(),
                    drone.getBaseLatitude(), drone.getBaseLongitude());
            distanceKm = toPickup + pickupToDelivery + deliveryToBase;
        }

        // Drone speed: 40 km/h = 0.667 km/min
        double speedKmPerMin = 40.0 / 60.0;
        int minutes = (int) Math.ceil(distanceKm / speedKmPerMin);

        return Math.max(0, minutes);
    }

    /**
     * DTO for real-time tracking response
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TrackingResponse {
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
        private Integer estimatedArrivalMinutes; // Deprecated: use specific ETA fields below
        // Specific ETA fields for different roles
        private Integer estimatedPickupMinutes; // Time to reach pickup location (for merchant)
        private Integer estimatedDeliveryMinutes; // Time to reach delivery location (for user)
        private Integer estimatedReturnToBaseMinutes; // Time to return to base (for admin)
    }
}
