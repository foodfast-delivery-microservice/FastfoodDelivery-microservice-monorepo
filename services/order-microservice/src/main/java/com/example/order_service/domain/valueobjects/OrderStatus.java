package com.example.order_service.domain.valueobjects;

/**
 * Value Object representing the status of an order throughout its lifecycle.
 * This enum defines all possible states an order can be in.
 */
public enum OrderStatus {
    /** Order has been created but not yet confirmed by merchant */
    PENDING,

    /** Order has been confirmed by merchant */
    CONFIRMED,

    /** Payment has been successfully processed */
    PAID,

    /** Order is being prepared/processed by merchant */
    PROCESSING,

    /** Order has been shipped (traditional delivery) */
    SHIPPED,

    /** Order is currently being delivered (e.g., by drone) */
    DELIVERING,

    /** Order has been successfully delivered to customer */
    DELIVERED,

    /** Order has been cancelled */
    CANCELLED,

    /** Payment has been refunded */
    REFUNDED;

    /**
     * Check if this status represents a final state (terminal state)
     */
    public boolean isFinalState() {
        return this == DELIVERED || this == CANCELLED || this == REFUNDED;
    }

    /**
     * Check if order can be cancelled from this status
     */
    public boolean canBeCancelled() {
        return this == PENDING || this == CONFIRMED ||
                this == PAID || this == PROCESSING;
    }

    /**
     * Check if order is in an active delivery state
     */
    public boolean isInDelivery() {
        return this == SHIPPED || this == DELIVERING;
    }
}
