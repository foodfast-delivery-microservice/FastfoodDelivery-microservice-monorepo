package com.example.order_service.infrastructure.messaging;

import com.example.order_service.infrastructure.event.RestaurantValidationFailedEvent;
import com.example.order_service.infrastructure.event.RestaurantValidationPassedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Event publisher for Order Service to publish validation results.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String USER_EVENTS_EXCHANGE = "user.events";

    public void publishRestaurantValidationPassed(RestaurantValidationPassedEvent event) {
        try {
            log.info("Publishing RestaurantValidationPassedEvent for restaurantId: {}", event.getRestaurantId());
            rabbitTemplate.convertAndSend(
                    USER_EVENTS_EXCHANGE,
                    "restaurant.validation.passed",
                    event);
        } catch (Exception e) {
            log.error("Failed to publish RestaurantValidationPassedEvent for restaurantId: {}",
                    event.getRestaurantId(), e);
        }
    }

    public void publishRestaurantValidationFailed(RestaurantValidationFailedEvent event) {
        try {
            log.info("Publishing RestaurantValidationFailedEvent for restaurantId: {}", event.getRestaurantId());
            rabbitTemplate.convertAndSend(
                    USER_EVENTS_EXCHANGE,
                    "restaurant.validation.failed",
                    event);
        } catch (Exception e) {
            log.error("Failed to publish RestaurantValidationFailedEvent for restaurantId: {}",
                    event.getRestaurantId(), e);
        }
    }
}
