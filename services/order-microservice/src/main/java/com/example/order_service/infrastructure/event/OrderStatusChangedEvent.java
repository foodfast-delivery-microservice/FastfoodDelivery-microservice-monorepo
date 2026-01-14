package com.example.order_service.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event payload for order status changes.
 * This event is published when an order transitions from one status to another.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusChangedEvent {
    private Long orderId;
    private String orderCode;
    private Long userId;
    private String oldStatus;
    private String newStatus;
    private String note;
    private String timestamp;
}
