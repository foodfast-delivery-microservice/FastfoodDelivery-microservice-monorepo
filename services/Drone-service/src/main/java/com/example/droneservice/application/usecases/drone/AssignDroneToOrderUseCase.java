package com.example.droneservice.application.usecases.drone;

import com.example.droneservice.application.DTOs.drone.AssignDroneRequest;
import com.example.droneservice.application.DTOs.mission.MissionResponse;

import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.entities.DroneMission;
import com.example.droneservice.domain.entities.OutboxEvent;
import com.example.droneservice.domain.valueobjects.*;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.domain.repository.DroneRepository;
import com.example.droneservice.domain.repository.OutboxEventRepository;
import com.example.droneservice.infrastructure.event.DroneAssignedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        private final OutboxEventRepository outboxEventRepository;
        private final ObjectMapper objectMapper;

        private static final double AVERAGE_DRONE_SPEED_KMH = 40.0; // 40 km/h

        @Transactional
        public MissionResponse execute(AssignDroneRequest request) {
                log.info("🚁 Assigning drone to order: {}", request.getOrderId());

                // Find available drone (đã validate pin đủ cho toàn bộ lộ trình)
                Drone drone = getAvailableDroneUseCase.execute(
                                request.getPickupLatitude(),
                                request.getPickupLongitude(),
                                request.getDeliveryLatitude(),
                                request.getDeliveryLongitude())
                                .orElseThrow(() -> new IllegalStateException(
                                                "No available drones for this delivery. " +
                                                                "All drones either have insufficient battery or are not idle."));

                // Create Coordinates for locations
                Coordinates pickupLocation = new Coordinates(
                                request.getPickupLatitude(), request.getPickupLongitude());
                Coordinates deliveryLocation = new Coordinates(
                                request.getDeliveryLatitude(), request.getDeliveryLongitude());

                // Calculate total distance using Coordinates
                double totalDistance = drone.getBaseLocation().distanceTo(pickupLocation)
                                + pickupLocation.distanceTo(deliveryLocation)
                                + deliveryLocation.distanceTo(drone.getBaseLocation());

                // Validate battery using BatteryLevel value object
                if (!drone.getBatteryLevel().canSupport(totalDistance, 10.0)) {
                        log.error("❌ Battery validation failed! Drone {} has insufficient battery",
                                        drone.getSerialNumber().getValue());
                        throw new IllegalStateException(
                                        String.format("Drone %s has insufficient battery for this mission",
                                                        drone.getSerialNumber().getValue()));
                }

                log.info("✅ Battery validation passed: Drone {} can complete mission",
                                drone.getSerialNumber().getValue());

                // Calculate mission details (sử dụng totalDistance đã tính ở trên)

                int estimatedDuration = (int) Math.ceil((totalDistance / AVERAGE_DRONE_SPEED_KMH) * 60); // Convert to
                                                                                                         // minutes

                // Create mission using value objects
                DroneMission mission = new DroneMission();
                mission.setDrone(drone);
                mission.setOrderId(request.getOrderId());
                mission.setPickupLocation(pickupLocation);
                mission.setDeliveryLocation(deliveryLocation);
                mission.setStatus(Status.ASSIGNED);
                mission.setDistanceKm(totalDistance);
                mission.setEstimatedDurationMinutes(estimatedDuration);
                mission.setStartedAt(LocalDateTime.now());

                DroneMission savedMission = missionRepository.save(mission);

                // Update drone state to DELIVERING
                drone.setState(State.DELIVERING);
                droneRepository.save(drone);

                log.info("✅ Drone {} assigned to order {}. Mission ID: {}, Distance: {:.2f}km, ETA: {} minutes",
                                drone.getSerialNumber(), request.getOrderId(), savedMission.getId(),
                                totalDistance, estimatedDuration);

                // Create OutboxEvent for DRONE_ASSIGNED event
                createDroneAssignedOutboxEvent(savedMission, drone, estimatedDuration);

                return mapToResponse(savedMission, drone);
        }

        /**
         * Create OutboxEvent for DRONE_ASSIGNED event
         * OutboxEventRelay will publish it to notify Order Service
         */
        private void createDroneAssignedOutboxEvent(DroneMission mission, Drone drone, Integer estimatedDurationMinutes) {
                try {
                        // Create event DTO with serializable fields
                        DroneAssignedEvent eventDTO = DroneAssignedEvent.builder()
                                        .orderId(mission.getOrderId())
                                        .droneId(drone.getId())
                                        .droneSerialNumber(drone.getSerialNumber())
                                        .missionId(mission.getId())
                                        .estimatedArrival(LocalDateTime.now().plusMinutes(estimatedDurationMinutes))
                                        .estimatedDurationMinutes(estimatedDurationMinutes)
                                        .build();

                        String payloadJson = objectMapper.writeValueAsString(eventDTO);

                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                        .aggregateType("Mission")
                                        .aggregateId(mission.getId().toString())
                                        .type("DroneAssigned")
                                        .payload(payloadJson)
                                        .status(EventStatus.NEW)
                                        .createdAt(LocalDateTime.now())
                                        .build();

                        outboxEventRepository.save(outboxEvent);
                        log.debug("Created DroneAssigned outbox event for missionId: {}", mission.getId());
                } catch (JsonProcessingException e) {
                        log.error("Failed to serialize DroneAssigned event payload for missionId: {}", mission.getId(), e);
                        throw new RuntimeException("Failed to create outbox event", e);
                }
        }

        private MissionResponse mapToResponse(DroneMission mission, Drone drone) {
                return MissionResponse.builder()
                                .id(mission.getId())
                                .droneId(drone.getId())
                                .droneSerialNumber(drone.getSerialNumber())
                                .orderId(mission.getOrderId())
                                .pickupLatitude(mission.getPickupLocation() != null
                                                ? mission.getPickupLocation().getLatitude()
                                                : null)
                                .pickupLongitude(mission.getPickupLocation() != null
                                                ? mission.getPickupLocation().getLongitude()
                                                : null)
                                .deliveryLatitude(mission.getDeliveryLocation() != null
                                                ? mission.getDeliveryLocation().getLatitude()
                                                : null)
                                .deliveryLongitude(mission.getDeliveryLocation() != null
                                                ? mission.getDeliveryLocation().getLongitude()
                                                : null)
                                .status(mission.getStatus())
                                .distanceKm(mission.getDistanceKm())
                                .estimatedDurationMinutes(mission.getEstimatedDurationMinutes())
                                .startedAt(mission.getStartedAt())
                                .completedAt(mission.getCompletedAt())
                                .build();
        }
}
