package com.example.demo.infrastructure.messaging.event;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Event received from Order Service when restaurant deletion validation fails.
 * Indicates active orders exist and deletion must be rolled back.
 */
@Value
@Builder
public class RestaurantValidationFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    Long restaurantId;
    String reason;
    Long activeOrderCount;
    List<String> activeStatuses;
    Instant occurredAt;
}
