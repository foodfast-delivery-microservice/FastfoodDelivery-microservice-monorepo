package com.example.demo.infrastructure.messaging.event;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;

/**
 * Event received from Order Service when restaurant deletion validation passes.
 * Indicates no active orders exist and deletion can proceed.
 */
@Value
@Builder
public class RestaurantValidationPassedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    Long restaurantId;
    Instant occurredAt;
}
