package com.example.productservice.domain.valueobjects;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object representing a product price.
 * Immutable and encapsulates business rules for price operations.
 */
@Getter
@EqualsAndHashCode
public class Price {
    private final BigDecimal amount;
    private final String currency;

    /**
     * Create a new Price value object
     * 
     * @param amount   The price amount
     * @param currency The currency code (e.g., "VND", "USD")
     * @throws IllegalArgumentException if amount is null or negative, or currency is invalid
     */
    public Price(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative: " + amount);
        }

        if (currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be blank");
        }

        // Standardize to 2 decimal places for consistency
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.toUpperCase();
    }

    /**
     * Convenience constructor for VND currency (most common in the system)
     */
    public Price(BigDecimal amount) {
        this(amount, "VND");
    }

    /**
     * Multiply price by quantity
     */
    public Price multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + quantity);
        }
        return new Price(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }

    /**
     * Check if price is zero
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if price is greater than another
     */
    public boolean isGreaterThan(Price other) {
        validateCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Validate that currencies match for operations
     */
    private void validateCurrency(Price other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public String toString() {
        return amount.toString() + " " + currency;
    }
}
