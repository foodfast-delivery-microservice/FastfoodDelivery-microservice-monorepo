package com.example.order_service.infrastructure.event;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;

/**
 * Event published when restaurant deletion validation passes.
 * Signals User Service that no active orders exist and deletion can proceed.
 */
@Value
@Builder
public class RestaurantValidationPassedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    Long restaurantId;
    Instant occurredAt;
}
