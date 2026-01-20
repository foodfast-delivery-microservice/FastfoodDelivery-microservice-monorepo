package com.example.productservice.domain.valueobjects;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

/**
 * Value Object representing product stock quantity.
 * Immutable and encapsulates business rules for stock operations.
 */
@Getter
@EqualsAndHashCode
public class Stock {
    private final Integer quantity;

    /**
     * Create a new Stock value object
     * 
     * @param quantity The stock quantity
     * @throws IllegalArgumentException if quantity is null or negative
     */
    public Stock(Integer quantity) {
        Objects.requireNonNull(quantity, "Stock quantity cannot be null");

        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative: " + quantity);
        }

        this.quantity = quantity;
    }

    /**
     * Check if stock can be deducted by the specified amount
     * 
     * @param amount The amount to deduct
     * @return true if stock is sufficient, false otherwise
     */
    public boolean canDeduct(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Deduction amount cannot be negative: " + amount);
        }
        return this.quantity >= amount;
    }

    /**
     * Deduct stock by the specified amount
     * 
     * @param amount The amount to deduct
     * @return New Stock object with reduced quantity
     * @throws IllegalArgumentException if amount is negative or insufficient stock
     */
    public Stock deduct(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Deduction amount cannot be negative: " + amount);
        }
        if (!canDeduct(amount)) {
            throw new IllegalArgumentException(
                    "Insufficient stock: cannot deduct " + amount + " from " + quantity);
        }
        return new Stock(this.quantity - amount);
    }

    /**
     * Restore stock by the specified amount
     * 
     * @param amount The amount to restore
     * @return New Stock object with increased quantity
     * @throws IllegalArgumentException if amount is negative
     */
    public Stock restore(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Restore amount cannot be negative: " + amount);
        }
        return new Stock(this.quantity + amount);
    }

    /**
     * Check if stock is available (greater than zero)
     */
    public boolean isAvailable() {
        return this.quantity > 0;
    }

    /**
     * Check if stock is zero
     */
    public boolean isZero() {
        return this.quantity == 0;
    }

    /**
     * Check if stock is greater than a threshold
     */
    public boolean isGreaterThan(int threshold) {
        return this.quantity > threshold;
    }

    @Override
    public String toString() {
        return quantity.toString();
    }
}
