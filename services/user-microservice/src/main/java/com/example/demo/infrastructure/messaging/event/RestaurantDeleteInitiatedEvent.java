package com.example.demo.infrastructure.messaging.event;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;

/**
 * Event published when restaurant deletion is initiated.
 * This triggers the validation phase in Order Service.
 */
@Value
@Builder
public class RestaurantDeleteInitiatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    Long restaurantId;
    Long merchantId;
    Instant occurredAt;
}
