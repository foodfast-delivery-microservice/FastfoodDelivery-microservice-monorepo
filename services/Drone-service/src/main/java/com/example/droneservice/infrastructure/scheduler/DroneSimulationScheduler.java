package com.example.droneservice.infrastructure.scheduler;

import com.example.droneservice.application.usecases.drone.SimulateDroneMovementUseCase;
import com.example.droneservice.domain.entities.DroneMission;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.domain.valueobjects.Status;
import com.example.droneservice.infrastructure.config.RabbitMQConfig;
import com.example.droneservice.infrastructure.event.DeliveryCompletedEvent;
import com.example.droneservice.infrastructure.event.DroneStatusUpdateEvent;
import com.example.droneservice.infrastructure.util.HaversineDistanceCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that runs every 2 seconds to simulate drone movement
 * This is the heart of the drone simulation system
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DroneSimulationScheduler {

    private final DroneMissionRepository missionRepository;
    private final SimulateDroneMovementUseCase simulateMovementUseCase;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Log when scheduler is initialized (runs once on startup)
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("✅ DroneSimulationScheduler initialized - Will run every 2 seconds");
    }

    /**
     * Run every 2 seconds (2000ms)
     * Simulates movement for all active missions
     */
    @Transactional
    @Scheduled(fixedRate = 2000)
    public void simulateActiveMissions() {
        // Find all active missions (ASSIGNED or IN_PROGRESS)
        // Note: IN_PROGRESS includes missions where drone is RETURNING to base
        List<DroneMission> activeMissions = missionRepository.findByStatusIn(
                List.of(Status.ASSIGNED, Status.IN_PROGRESS));

        // Also include missions where drone is RETURNING but status might not be
        // updated yet
        // This ensures drones returning to base continue to be simulated
        // Note: When drone state = RETURNING, mission status is still IN_PROGRESS
        // So we need to explicitly check for RETURNING drones
        List<DroneMission> returningMissions = missionRepository.findAll().stream()
                .filter(m -> {
                    if (m.getDrone() == null)
                        return false;
                    var state = m.getDrone().getState();
                    var status = m.getStatus();
                    // Include missions where drone is RETURNING and not completed
                    return state == com.example.droneservice.domain.valueobjects.State.RETURNING
                            && status != Status.COMPLETED
                            && status != Status.CANCELLED;
                })
                .filter(m -> {
                    // Avoid duplicates - check if mission is already in activeMissions
                    return activeMissions.stream().noneMatch(am -> am.getId().equals(m.getId()));
                })
                .toList();

        // Combine both lists
        List<DroneMission> allActiveMissions = new java.util.ArrayList<>(activeMissions);
        allActiveMissions.addAll(returningMissions);

        if (allActiveMissions.isEmpty()) {
            log.trace("🔄 Scheduler running - No active missions");
            return; // No active missions, skip
        }

        if (!returningMissions.isEmpty()) {
            log.info("🔄 Simulating {} active drone missions ({} regular + {} returning)",
                    allActiveMissions.size(), activeMissions.size(), returningMissions.size());
        } else {
            log.info("🔄 Simulating {} active drone missions", allActiveMissions.size());
        }

        for (DroneMission mission : allActiveMissions) {
            try {
                // Get status and drone state before simulation
                Status statusBefore = mission.getStatus();
                var droneStateBefore = mission.getDrone().getState();

                // Simulate movement
                simulateMovementUseCase.execute(mission.getId());

                // Refresh mission to get updated data
                mission = missionRepository.findById(mission.getId()).orElse(null);
                if (mission == null)
                    continue;

                // Refresh drone to get updated state
                var drone = mission.getDrone();
                if (drone == null) {
                    log.warn("Drone not found for mission {}", mission.getId());
                    continue;
                }
                var droneStateAfter = drone.getState();

                // Publish status update event
                publishStatusUpdate(mission);

                // Check if drone just arrived at delivery location (state changed to RETURNING
                // from DELIVERING)
                // Hoặc đang ở DELIVERING và mission status = IN_PROGRESS (đang giao hàng)
                if (droneStateBefore == com.example.droneservice.domain.valueobjects.State.DELIVERING
                        && droneStateAfter == com.example.droneservice.domain.valueobjects.State.RETURNING) {
                    // Drone vừa giao hàng xong, chuyển sang RETURNING
                    log.info("📦 Drone {} đã giao hàng xong cho order {} - Gửi event để order status = 'delivered'",
                            drone.getSerialNumber().getValue(), mission.getOrderId());
                    publishOrderDelivered(mission);
                } else if (droneStateAfter == com.example.droneservice.domain.valueobjects.State.DELIVERING
                        && mission.getStatus() == Status.IN_PROGRESS) {
                    // Drone đang giao hàng (state = DELIVERING, status = IN_PROGRESS)
                    // Check xem có gần delivery location không (trong vòng 100m)
                    double distanceToDelivery = calculateDistanceToDelivery(mission, drone);
                    if (distanceToDelivery <= 0.1) { // 100 meters
                        log.info(
                                "📦 Drone {} đang giao hàng cho order {} (cách {:.2f}km) - Gửi event để order status = 'delivered'",
                                drone.getSerialNumber().getValue(), mission.getOrderId(), distanceToDelivery);
                        publishOrderDelivered(mission);
                    }
                }

                // Check if mission just completed
                if (statusBefore != Status.COMPLETED && mission.getStatus() == Status.COMPLETED) {
                    publishDeliveryCompleted(mission);
                }

            } catch (Exception e) {
                log.error("❌ Error simulating mission {}: {}",
                        mission.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Publish drone status update event for real-time tracking
     */
    private void publishStatusUpdate(DroneMission mission) {
        var drone = mission.getDrone();
        DroneStatusUpdateEvent event = DroneStatusUpdateEvent.builder()
                .missionId(mission.getId())
                .orderId(mission.getOrderId())
                .droneId(drone.getId())
                .droneSerialNumber(drone.getSerialNumber().getValue())
                .currentLatitude(drone.getCurrentLocation().getLatitude())
                .currentLongitude(drone.getCurrentLocation().getLongitude())
                .batteryLevel(drone.getBatteryLevel().getValue())
                .status(mission.getStatus())
                .estimatedArrivalMinutes(calculateETA(mission))
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DRONE_EXCHANGE,
                RabbitMQConfig.DRONE_STATUS_UPDATE_ROUTING_KEY,
                event);

        log.trace("📡 Published status update for mission {} (Order {})",
                mission.getId(), mission.getOrderId());
    }

    /**
     * Publish order delivered event (khi drone state = DELIVERING và đang giao
     * hàng)
     * Event này sẽ được order service nhận để đổi order status = "delivered"
     */
    private void publishOrderDelivered(DroneMission mission) {
        DeliveryCompletedEvent event = DeliveryCompletedEvent.builder()
                .orderId(mission.getOrderId())
                .missionId(mission.getId())
                .droneId(mission.getDrone().getId())
                .completedAt(LocalDateTime.now()) // Thời điểm giao hàng
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DRONE_EXCHANGE,
                RabbitMQConfig.DELIVERY_COMPLETED_ROUTING_KEY,
                event);

        log.info("✅ Published ORDER_DELIVERED event for order {} - Order status sẽ được đổi thành 'delivered'",
                mission.getOrderId());
    }

    /**
     * Publish delivery completed event (khi mission hoàn thành, drone về base)
     */
    private void publishDeliveryCompleted(DroneMission mission) {
        DeliveryCompletedEvent event = DeliveryCompletedEvent.builder()
                .orderId(mission.getOrderId())
                .missionId(mission.getId())
                .droneId(mission.getDrone().getId())
                .completedAt(mission.getCompletedAt())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DRONE_EXCHANGE,
                RabbitMQConfig.DELIVERY_COMPLETED_ROUTING_KEY,
                event);

        log.info("✅ Published DELIVERY_COMPLETED event for order {}", mission.getOrderId());
    }

    /**
     * Calculate distance from drone current position to delivery location
     */
    private double calculateDistanceToDelivery(DroneMission mission,
            com.example.droneservice.domain.entities.Drone drone) {
        var currentLocation = drone.getCurrentLocation();
        var deliveryLocation = mission.getDeliveryLocation();

        if (currentLocation == null || deliveryLocation == null) {
            return Double.MAX_VALUE;
        }

        return HaversineDistanceCalculator.calculate(
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                deliveryLocation.getLatitude(), deliveryLocation.getLongitude());
    }

    /**
     * Calculate estimated time of arrival in minutes
     */
    private Integer calculateETA(DroneMission mission) {
        if (mission.getStartedAt() == null || mission.getEstimatedDurationMinutes() == null) {
            return null;
        }

        LocalDateTime estimatedCompletion = mission.getStartedAt()
                .plusMinutes(mission.getEstimatedDurationMinutes());

        long minutesRemaining = java.time.Duration.between(
                LocalDateTime.now(),
                estimatedCompletion).toMinutes();

        return Math.max(0, (int) minutesRemaining);
    }
}
