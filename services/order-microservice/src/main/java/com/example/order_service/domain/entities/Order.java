package com.example.order_service.domain.entities;

import com.example.order_service.domain.valueobjects.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure domain entity representing an Order.
 * Contains business logic and domain rules, independent of persistence
 * framework.
 * No JPA annotations - this is a pure business object.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private Long id;
    private OrderCode orderCode;
    private Long userId;
    private Long merchantId;
    private OrderStatus status;

    // Money value objects
    private Money subtotal;
    private Money discount;
    private Money shippingFee;
    private Money grandTotal;

    private String note;
    private DeliveryAddress deliveryAddress;

    private LocalDateTime createdAt;
    private LocalDateTime processingStartedAt;

    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    // ==================== Business Logic Methods ====================

    /**
     * Add an order item to this order
     */
    public void addOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        this.orderItems.add(orderItem);
    }

    /**
     * Remove an order item from this order
     */
    public void removeOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        this.orderItems.remove(orderItem);
    }

    /**
     * Calculate order totals based on order items
     * Business Rule: subtotal = sum of all line totals
     * Business Rule: grandTotal = subtotal + shippingFee - discount
     */
    public void calculateTotals() {
        // Get currency from first item or default to VND
        String currency = this.subtotal != null ? this.subtotal.getCurrency() : "VND";

        // Calculate subtotal from order items
        Money calculatedSubtotal = orderItems.stream()
                .map(OrderItem::getLineTotal)
                .reduce(Money.zero(currency), Money::add);

        this.subtotal = calculatedSubtotal;

        // Ensure discount and shipping fee are initialized
        if (this.discount == null) {
            this.discount = Money.zero(currency);
        }
        if (this.shippingFee == null) {
            this.shippingFee = Money.zero(currency);
        }

        // Calculate grand total
        this.grandTotal = this.subtotal
                .add(this.shippingFee)
                .subtract(this.discount);
    }

    /**
     * Business Rule: Order can be cancelled only if not yet shipped/delivering
     */
    public boolean canBeCancelled() {
        return status.canBeCancelled();
    }

    /**
     * Cancel the order
     * 
     * @throws IllegalStateException if order cannot be cancelled
     */
    public void cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException(
                    "Order cannot be cancelled in current status: " + status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * Confirm the order (merchant accepts it)
     * Business Rule: Only PENDING orders can be confirmed
     */
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending orders can be confirmed. Current status: " + status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * Mark order as paid
     * Business Rule: Only CONFIRMED orders can be marked as paid
     */
    public void markAsPaid() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only confirmed orders can be paid. Current status: " + status);
        }
        this.status = OrderStatus.PAID;
    }

    /**
     * Start processing the order (preparing food)
     * Business Rule: Only PAID orders can start processing
     */
    public void startProcessing() {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException(
                    "Only paid orders can start processing. Current status: " + status);
        }
        this.status = OrderStatus.PROCESSING;
        this.processingStartedAt = LocalDateTime.now();
    }

    /**
     * Mark order as shipped (traditional delivery)
     * Business Rule: Only PROCESSING orders can be shipped
     * Flow: PAID → PROCESSING → SHIPPED
     */
    public void markAsShipped() {
        if (status != OrderStatus.PROCESSING) {
            throw new IllegalStateException(
                    String.format("Only processing orders can be shipped. Current status: %s. " +
                            "Flow: PAID → PROCESSING → SHIPPED", status));
        }
        this.status = OrderStatus.SHIPPED;
    }

    /**
     * Mark order as delivering (e.g., drone delivery)
     * Business Rule: Only PROCESSING orders can be marked as delivering
     */
    public void markAsDelivering() {
        if (status != OrderStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only processing orders can be marked as delivering. Current status: " + status);
        }
        this.status = OrderStatus.DELIVERING;
    }

    /**
     * Mark order as delivered
     * Business Rule: Only SHIPPED or DELIVERING orders can be delivered
     */
    public void markAsDelivered() {
        if (!status.isInDelivery()) {
            throw new IllegalStateException(
                    String.format("Only shipped or delivering orders can be delivered. Current status: %s",
                            status));
        }
        this.status = OrderStatus.DELIVERED;
    }

    /**
     * Request refund for the order
     * Business Rule: Order must be DELIVERED or CANCELLED to refund
     */
    public void refund() {
        if (status != OrderStatus.DELIVERED && status != OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Only delivered or cancelled orders can be refunded. Current status: " + status);
        }
        this.status = OrderStatus.REFUNDED;
    }

    /**
     * Check if order is in a final state
     */
    public boolean isFinalState() {
        return status.isFinalState();
    }

}
