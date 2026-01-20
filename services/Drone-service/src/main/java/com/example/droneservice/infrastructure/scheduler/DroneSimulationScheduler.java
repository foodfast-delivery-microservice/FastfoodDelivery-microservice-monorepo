package com.example.droneservice.infrastructure.scheduler;

import com.example.droneservice.application.usecases.drone.SimulateDroneMovementUseCase;
import com.example.droneservice.domain.entities.DroneMission;
import com.example.droneservice.domain.entities.OutboxEvent;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.domain.repository.OutboxEventRepository;
import com.example.droneservice.domain.valueobjects.EventStatus;
import com.example.droneservice.domain.valueobjects.Status;
import com.example.droneservice.infrastructure.event.DeliveryCompletedEvent;
import com.example.droneservice.infrastructure.event.DroneStatusUpdateEvent;
import com.example.droneservice.infrastructure.util.HaversineDistanceCalculator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

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

                // Create OutboxEvent for status update
                createStatusUpdateOutboxEvent(mission);

                // Check if drone just arrived at delivery location (state changed to RETURNING
                // from DELIVERING)
                // Hoặc đang ở DELIVERING và mission status = IN_PROGRESS (đang giao hàng)
                if (droneStateBefore == com.example.droneservice.domain.valueobjects.State.DELIVERING
                        && droneStateAfter == com.example.droneservice.domain.valueobjects.State.RETURNING) {
                    // Drone vừa giao hàng xong, chuyển sang RETURNING
                    log.info("📦 Drone {} đã giao hàng xong cho order {} - Creating event để order status = 'delivered'",
                            drone.getSerialNumber().getValue(), mission.getOrderId());
                    createDeliveryCompletedOutboxEvent(mission);
                } else if (droneStateAfter == com.example.droneservice.domain.valueobjects.State.DELIVERING
                        && mission.getStatus() == Status.IN_PROGRESS) {
                    // Drone đang giao hàng (state = DELIVERING, status = IN_PROGRESS)
                    // Check xem có gần delivery location không (trong vòng 100m)
                    double distanceToDelivery = calculateDistanceToDelivery(mission, drone);
                    if (distanceToDelivery <= 0.1) { // 100 meters
                        log.info(
                                "📦 Drone {} đang giao hàng cho order {} (cách {:.2f}km) - Creating event để order status = 'delivered'",
                                drone.getSerialNumber().getValue(), mission.getOrderId(), distanceToDelivery);
                        createDeliveryCompletedOutboxEvent(mission);
                    }
                }

                // Check if mission just completed
                if (statusBefore != Status.COMPLETED && mission.getStatus() == Status.COMPLETED) {
                    createDeliveryCompletedOutboxEvent(mission);
                }

            } catch (Exception e) {
                log.error("❌ Error simulating mission {}: {}",
                        mission.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Create OutboxEvent for drone status update event
     */
    private void createStatusUpdateOutboxEvent(DroneMission mission) {
        try {
            var drone = mission.getDrone();
            DroneStatusUpdateEvent eventDTO = DroneStatusUpdateEvent.builder()
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

            String payloadJson = objectMapper.writeValueAsString(eventDTO);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Mission")
                    .aggregateId(mission.getId().toString())
                    .type("DroneStatusUpdate")
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.trace("Created status update outbox event for mission {} (Order {})",
                    mission.getId(), mission.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DroneStatusUpdate event payload for missionId: {}", mission.getId(), e);
            // Don't throw exception - status updates are not critical
        }
    }

    /**
     * Create OutboxEvent for delivery completed event
     */
    private void createDeliveryCompletedOutboxEvent(DroneMission mission) {
        try {
            DeliveryCompletedEvent eventDTO = DeliveryCompletedEvent.builder()
                    .orderId(mission.getOrderId())
                    .missionId(mission.getId())
                    .droneId(mission.getDrone().getId())
                    .completedAt(mission.getCompletedAt() != null ? mission.getCompletedAt() : LocalDateTime.now())
                    .build();

            String payloadJson = objectMapper.writeValueAsString(eventDTO);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Mission")
                    .aggregateId(mission.getId().toString())
                    .type("DeliveryCompleted")
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Created DeliveryCompleted outbox event for order {}", mission.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DeliveryCompleted event payload for missionId: {}", mission.getId(), e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
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
