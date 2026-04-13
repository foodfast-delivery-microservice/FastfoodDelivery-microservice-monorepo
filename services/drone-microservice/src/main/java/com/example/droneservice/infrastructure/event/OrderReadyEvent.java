package com.example.droneservice.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event received from Order Service when order is ready to ship
 * This is published after:
 * 1. Merchant confirmed order
 * 2. User paid successfully
 * 3. Food preparation completed
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderReadyEvent {

    private Long orderId;

    private Long merchantId;

    private String deliveryMethod; // "DRONE" or "STANDARD"

    // Pickup location (restaurant)
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;

    // Delivery location (customer)
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String deliveryAddress;
}
