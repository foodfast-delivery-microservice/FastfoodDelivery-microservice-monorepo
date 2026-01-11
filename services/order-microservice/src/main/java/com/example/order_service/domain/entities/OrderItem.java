package com.example.order_service.domain.entities;

import com.example.order_service.domain.valueobjects.Money;
import lombok.*;

/**
 * Pure domain entity representing an Order Item.
 * Contains business logic for line total calculation.
 * No JPA annotations - this is a pure business object.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private Long id;
    private Long productId;
    private Long merchantId;
    private String productName;
    private Money unitPrice;
    private Integer quantity;
    private Money lineTotal;

    // ==================== Business Logic Methods ====================

    /**
     * Calculate line total based on unit price and quantity
     * Business Rule: lineTotal = unitPrice × quantity
     */
    public void calculateLineTotal() {
        if (unitPrice == null) {
            throw new IllegalStateException("Unit price must be set before calculating line total");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalStateException("Quantity must be positive before calculating line total");
        }

        this.lineTotal = unitPrice.multiply(quantity);
    }

    /**
     * Update quantity and recalculate line total
     */
    public void updateQuantity(Integer newQuantity) {
        if (newQuantity == null || newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + newQuantity);
        }
        this.quantity = newQuantity;
        calculateLineTotal();
    }

    /**
     * Validate that this order item is valid
     */
    public void validate() {
        if (productId == null) {
            throw new IllegalStateException("Product ID cannot be null");
        }
        if (merchantId == null) {
            throw new IllegalStateException("Merchant ID cannot be null");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalStateException("Product name cannot be blank");
        }
        if (unitPrice == null) {
            throw new IllegalStateException("Unit price cannot be null");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalStateException("Quantity must be positive");
        }
    }

}
