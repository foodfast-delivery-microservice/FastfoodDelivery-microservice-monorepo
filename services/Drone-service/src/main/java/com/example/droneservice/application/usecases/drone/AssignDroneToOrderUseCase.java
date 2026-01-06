package com.example.droneservice.application.usecase;

import com.example.droneservice.application.dto.AssignDroneRequest;
import com.example.droneservice.application.dto.MissionResponse;

import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.entities.DroneMission;
import com.example.droneservice.domain.valueobjects.*;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.domain.repository.DroneRepository;
import com.example.droneservice.infrastructure.config.RabbitMQConfig;
import com.example.droneservice.infrastructure.event.DroneAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
        private final RabbitTemplate rabbitTemplate;

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

                // Publish DRONE_ASSIGNED event to update order status to DELIVERING
                publishDroneAssignedEvent(savedMission, drone, estimatedDuration);

                return mapToResponse(savedMission, drone);
        }

        /**
         * Publish DRONE_ASSIGNED event to notify Order Service
         * Order Service will update order status to DELIVERING
         */
        private void publishDroneAssignedEvent(DroneMission mission, Drone drone, Integer estimatedDurationMinutes) {
                DroneAssignedEvent event = DroneAssignedEvent.builder()
                                .orderId(mission.getOrderId())
                                .droneId(drone.getId())
                                .droneSerialNumber(drone.getSerialNumber())
                                .missionId(mission.getId())
                                .estimatedArrival(LocalDateTime.now().plusMinutes(estimatedDurationMinutes))
                                .estimatedDurationMinutes(estimatedDurationMinutes)
                                .build();

                rabbitTemplate.convertAndSend(
                                RabbitMQConfig.DRONE_EXCHANGE,
                                RabbitMQConfig.DRONE_ASSIGNED_ROUTING_KEY,
                                event);

                log.info("📡 Published DRONE_ASSIGNED event for order {} - Order status sẽ được đổi thành 'DELIVERING'",
                                mission.getOrderId());
        }

        private MissionResponse mapToResponse(DroneMission mission, Drone drone) {
                return MissionResponse.builder()
                                .id(mission.getId())
                                .droneId(drone.getId())
                                .droneSerialNumber(drone.getSerialNumber().getValue())
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
