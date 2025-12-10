package com.example.order_service.infrastructure.listener;

import com.example.order_service.application.usecase.ValidateRestaurantDeleteUseCase;
import com.example.order_service.infrastructure.config.RabbitMQConfig;
import com.example.order_service.infrastructure.event.RestaurantDeleteInitiatedEvent;
import com.example.order_service.infrastructure.event.RestaurantValidationFailedEvent;
import com.example.order_service.infrastructure.event.RestaurantValidationPassedEvent;
import com.example.order_service.infrastructure.messaging.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Listener for restaurant deletion initiated events from User Service.
 * Validates if restaurant can be deleted and publishes result.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantDeleteValidationListener {

    private final ValidateRestaurantDeleteUseCase validateRestaurantDeleteUseCase;
    private final OrderEventPublisher eventPublisher;

    @RabbitListener(queues = RabbitMQConfig.RESTAURANT_DELETE_INITIATED_QUEUE)
    public void handleRestaurantDeleteInitiated(RestaurantDeleteInitiatedEvent event) {
        Long merchantId = event.getMerchantId();
        Long restaurantId = event.getRestaurantId();

        if (merchantId == null || restaurantId == null) {
            log.warn("RestaurantDeleteInitiatedEvent missing merchantId or restaurantId: {}", event);
            return;
        }

        log.info("Received RestaurantDeleteInitiatedEvent for restaurant {} (merchant {}). Validating...",
                restaurantId, merchantId);

        try {
            // Validate if restaurant can be deleted
            ValidateRestaurantDeleteUseCase.ValidationResult result = validateRestaurantDeleteUseCase
                    .execute(merchantId);

            if (result.isCanDelete()) {
                // No active orders - publish validation passed event
                RestaurantValidationPassedEvent passedEvent = RestaurantValidationPassedEvent.builder()
                        .restaurantId(restaurantId)
                        .occurredAt(Instant.now())
                        .build();

                eventPublisher.publishRestaurantValidationPassed(passedEvent);
                log.info("Validation passed for restaurant {}. No active orders found.", restaurantId);
            } else {
                // Active orders exist - publish validation failed event
                RestaurantValidationFailedEvent failedEvent = RestaurantValidationFailedEvent.builder()
                        .restaurantId(restaurantId)
                        .reason(result.getReason())
                        .activeOrderCount(result.getActiveOrderCount())
                        .activeStatuses(result.getActiveStatuses())
                        .occurredAt(Instant.now())
                        .build();

                eventPublisher.publishRestaurantValidationFailed(failedEvent);
                log.info("Validation failed for restaurant {}. Active orders: {}, Statuses: {}",
                        restaurantId, result.getActiveOrderCount(), result.getActiveStatuses());
            }
        } catch (Exception e) {
            log.error("Error validating restaurant deletion for restaurant {}", restaurantId, e);
            // Publish failure event on error
            RestaurantValidationFailedEvent failedEvent = RestaurantValidationFailedEvent.builder()
                    .restaurantId(restaurantId)
                    .reason("Validation error: " + e.getMessage())
                    .activeOrderCount(0L)
                    .activeStatuses(java.util.Collections.emptyList())
                    .occurredAt(Instant.now())
                    .build();

            eventPublisher.publishRestaurantValidationFailed(failedEvent);
        }
    }
}
