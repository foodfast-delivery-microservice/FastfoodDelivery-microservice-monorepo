package com.example.demo.infrastructure.listener;

import com.example.demo.application.usecase.CompleteRestaurantDeleteUseCase;
import com.example.demo.application.usecase.RollbackRestaurantDeleteUseCase;
import com.example.demo.infrastructure.config.RabbitMQConfig;
import com.example.demo.infrastructure.messaging.event.RestaurantValidationFailedEvent;
import com.example.demo.infrastructure.messaging.event.RestaurantValidationPassedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener for restaurant deletion validation results from Order Service.
 * Completes or rolls back the deletion based on validation outcome.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantValidationListener {

    private final CompleteRestaurantDeleteUseCase completeDeleteUseCase;
    private final RollbackRestaurantDeleteUseCase rollbackDeleteUseCase;

    @RabbitListener(queues = RabbitMQConfig.RESTAURANT_VALIDATION_PASSED_QUEUE)
    @Transactional
    public void handleValidationPassed(RestaurantValidationPassedEvent event) {
        Long restaurantId = event.getRestaurantId();
        if (restaurantId == null) {
            log.warn("RestaurantValidationPassedEvent missing restaurantId: {}", event);
            return;
        }

        log.info("Received RestaurantValidationPassedEvent for restaurant {}. Completing deletion.", restaurantId);

        try {
            completeDeleteUseCase.execute(restaurantId);
            log.info("Successfully completed deletion for restaurant {}", restaurantId);
        } catch (Exception e) {
            log.error("Failed to complete deletion for restaurant {}", restaurantId, e);
            throw e; // Rethrow to trigger message requeue
        }
    }

    @RabbitListener(queues = RabbitMQConfig.RESTAURANT_VALIDATION_FAILED_QUEUE)
    @Transactional
    public void handleValidationFailed(RestaurantValidationFailedEvent event) {
        Long restaurantId = event.getRestaurantId();
        if (restaurantId == null) {
            log.warn("RestaurantValidationFailedEvent missing restaurantId: {}", event);
            return;
        }

        String reason = String.format("Validation failed: %s (Active orders: %d, Statuses: %s)",
                event.getReason(),
                event.getActiveOrderCount(),
                event.getActiveStatuses());

        log.info("Received RestaurantValidationFailedEvent for restaurant {}. Rolling back deletion. Reason: {}",
                restaurantId, reason);

        try {
            rollbackDeleteUseCase.execute(restaurantId, reason);
            log.info("Successfully rolled back deletion for restaurant {}", restaurantId);
        } catch (Exception e) {
            log.error("Failed to rollback deletion for restaurant {}", restaurantId, e);
            throw e; // Rethrow to trigger message requeue
        }
    }
}
