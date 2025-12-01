package com.example.droneservice.infrastructure.scheduler;

import com.example.droneservice.application.usecase.SimulateDroneMovementUseCase;
import com.example.droneservice.domain.model.DroneMission;
import com.example.droneservice.domain.model.Status;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.infrastructure.config.RabbitMQConfig;
import com.example.droneservice.infrastructure.event.DeliveryCompletedEvent;
import com.example.droneservice.infrastructure.event.DroneStatusUpdateEvent;
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
     * Run every 2 seconds (2000ms)
     * Simulates movement for all active missions
     */
    @Scheduled(fixedRate = 2000)
    public void simulateActiveMissions() {
        // Find all active missions (ASSIGNED or IN_PROGRESS)
        List<DroneMission> activeMissions = missionRepository.findByStatusIn(
                List.of(Status.ASSIGNED, Status.IN_PROGRESS));

        if (activeMissions.isEmpty()) {
            return; // No active missions, skip
        }

        log.debug("🔄 Simulating {} active drone missions", activeMissions.size());

        for (DroneMission mission : activeMissions) {
            try {
                // Get status before simulation
                Status statusBefore = mission.getStatus();

                // Simulate movement
                simulateMovementUseCase.execute(mission.getId());

                // Refresh mission to get updated data
                mission = missionRepository.findById(mission.getId()).orElse(null);
                if (mission == null)
                    continue;

                // Publish status update event
                publishStatusUpdate(mission);

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
        DroneStatusUpdateEvent event = DroneStatusUpdateEvent.builder()
                .missionId(mission.getId())
                .orderId(mission.getOrderId())
                .droneId(mission.getDrone().getId())
                .droneSerialNumber(mission.getDrone().getSerialNumber())
                .currentLatitude(mission.getDrone().getCurrentLatitude())
                .currentLongitude(mission.getDrone().getCurrentLongitude())
                .batteryLevel(mission.getDrone().getBatteryLevel())
                .status(mission.getStatus())
                .estimatedArrivalMinutes(calculateETA(mission))
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DRONE_EXCHANGE,
                RabbitMQConfig.DRONE_STATUS_UPDATE_ROUTING_KEY,
                event);

        log.debug("📡 Published status update for mission {} (Order {})",
                mission.getId(), mission.getOrderId());
    }

    /**
     * Publish delivery completed event
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
