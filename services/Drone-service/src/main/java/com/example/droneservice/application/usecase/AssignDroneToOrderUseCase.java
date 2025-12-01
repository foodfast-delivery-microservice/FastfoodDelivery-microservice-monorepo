package com.example.droneservice.application.usecase;

import com.example.droneservice.application.dto.AssignDroneRequest;
import com.example.droneservice.application.dto.MissionResponse;
import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.DroneMission;
import com.example.droneservice.domain.model.State;
import com.example.droneservice.domain.model.Status;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.domain.repository.DroneRepository;
import com.example.droneservice.infrastructure.util.HaversineDistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case to assign a drone to a delivery order.
 * This creates a mission and updates the drone state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssignDroneToOrderUseCase {

    private final DroneRepository droneRepository;
    private final DroneMissionRepository missionRepository;
    private final GetAvailableDroneUseCase getAvailableDroneUseCase;

    private static final double AVERAGE_DRONE_SPEED_KMH = 40.0; // 40 km/h

    @Transactional
    public MissionResponse execute(AssignDroneRequest request) {
        log.info("Assigning drone to order: {}", request.getOrderId());

        // Find available drone
        Drone drone = getAvailableDroneUseCase.execute(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDeliveryLatitude(),
                request.getDeliveryLongitude())
                .orElseThrow(() -> new IllegalStateException("No available drones for this delivery"));

        // Calculate mission details
        double totalDistance = calculateTotalDistance(
                drone.getBaseLatitude(), drone.getBaseLongitude(),
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDeliveryLatitude(), request.getDeliveryLongitude());

        int estimatedDuration = (int) Math.ceil((totalDistance / AVERAGE_DRONE_SPEED_KMH) * 60); // Convert to minutes

        // Create mission
        DroneMission mission = new DroneMission();
        mission.setDrone(drone);
        mission.setOrderId(request.getOrderId());
        mission.setPickupLatitude(request.getPickupLatitude());
        mission.setPickupLongitude(request.getPickupLongitude());
        mission.setDeliveryLatitude(request.getDeliveryLatitude());
        mission.setDeliveryLongitude(request.getDeliveryLongitude());
        mission.setStatus(Status.ASSIGNED);
        mission.setDistanceKm(totalDistance);
        mission.setEstimatedDurationMinutes(estimatedDuration);
        mission.setStartedAt(LocalDateTime.now());

        DroneMission savedMission = missionRepository.save(mission);

        // Update drone state
        drone.setState(State.DELIVERING);
        droneRepository.save(drone);

        log.info("Drone {} assigned to order {}. Mission ID: {}, Distance: {:.2f}km, ETA: {} minutes",
                drone.getSerialNumber(), request.getOrderId(), savedMission.getId(),
                totalDistance, estimatedDuration);

        return mapToResponse(savedMission, drone);
    }

    private double calculateTotalDistance(Double baseLat, Double baseLon,
            Double pickupLat, Double pickupLon,
            Double deliveryLat, Double deliveryLon) {

        double baseToPickup = HaversineDistanceCalculator.calculate(baseLat, baseLon, pickupLat, pickupLon);
        double pickupToDelivery = HaversineDistanceCalculator.calculate(pickupLat, pickupLon, deliveryLat, deliveryLon);
        double deliveryToBase = HaversineDistanceCalculator.calculate(deliveryLat, deliveryLon, baseLat, baseLon);

        return baseToPickup + pickupToDelivery + deliveryToBase;
    }

    private MissionResponse mapToResponse(DroneMission mission, Drone drone) {
        return MissionResponse.builder()
                .id(mission.getId())
                .droneId(drone.getId())
                .droneSerialNumber(drone.getSerialNumber())
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
}
