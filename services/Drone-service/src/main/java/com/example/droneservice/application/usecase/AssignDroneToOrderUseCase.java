package com.example.droneservice.application.usecase;

import com.example.droneservice.application.dto.AssignDroneRequest;
import com.example.droneservice.application.dto.MissionResponse;
import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.DroneMission;
import com.example.droneservice.domain.model.State;
import com.example.droneservice.domain.model.Status;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.domain.repository.DroneRepository;
import com.example.droneservice.infrastructure.config.RabbitMQConfig;
import com.example.droneservice.infrastructure.event.DroneAssignedEvent;
import com.example.droneservice.infrastructure.util.HaversineDistanceCalculator;
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

        // Double-check: Validate battery one more time before assignment
        // Calculate total distance: Base → Pickup → Delivery → Base
        double totalDistance = calculateTotalDistance(
                drone.getBaseLatitude(), drone.getBaseLongitude(),
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDeliveryLatitude(), request.getDeliveryLongitude());
        
        // Validate battery: Pin cần = Tổng quãng đường × 2% mỗi km + 10% dự phòng
        double requiredBattery = totalDistance * 2.0; // 2% per km
        double minimumBatteryNeeded = requiredBattery + 10; // +10% reserve
        
        if (drone.getBatteryLevel() < minimumBatteryNeeded) {
            log.error("❌ Battery validation failed! Drone {} has {}% but needs {:.1f}%",
                    drone.getSerialNumber(), drone.getBatteryLevel(), minimumBatteryNeeded);
            throw new IllegalStateException(
                    String.format("Drone %s has insufficient battery: %d%% (Required: %.1f%%)",
                            drone.getSerialNumber(), drone.getBatteryLevel(), minimumBatteryNeeded));
        }
        
        log.info("✅ Battery validation passed: Drone {} has {}% (Required: {:.1f}%)",
                drone.getSerialNumber(), drone.getBatteryLevel(), minimumBatteryNeeded);

        // Calculate mission details (sử dụng totalDistance đã tính ở trên)

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

    private double calculateTotalDistance(Double baseLat, Double baseLon,
                                          Double pickupLat, Double pickupLon,
                                          Double deliveryLat, Double deliveryLon) {

        double baseToPickup = HaversineDistanceCalculator.calculate(baseLat, baseLon, pickupLat, pickupLon);
        double pickupToDelivery = HaversineDistanceCalculator.calculate(pickupLat, pickupLon, deliveryLat, deliveryLon);
        double deliveryToBase = HaversineDistanceCalculator.calculate(deliveryLat, deliveryLon, baseLat, baseLon);

        return baseToPickup + pickupToDelivery + deliveryToBase;
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
