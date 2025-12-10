package com.example.order_service.infrastructure.event;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;

/**
 * Event received from User Service when restaurant deletion is initiated.
 * Triggers validation to check if restaurant can be safely deleted.
 */
@Value
@Builder
public class RestaurantDeleteInitiatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    Long restaurantId;
    Long merchantId;
    Instant occurredAt;
}
