package com.example.order_service.domain.valueobjects;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object representing a monetary value with currency.
 * Immutable and encapsulates business rules for money operations.
 */
@Getter
@EqualsAndHashCode
public class Money {
    private final BigDecimal amount;
    private final String currency;

    /**
     * Create a new Money value object
     * 
     * @param amount   The monetary amount
     * @param currency The currency code (e.g., "VND", "USD")
     * @throws IllegalArgumentException if amount is null or negative, or currency
     *                                  is invalid
     */
    public Money(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
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
    public Money(BigDecimal amount) {
        this(amount, "VND");
    }

    /**
     * Convenience constructor from double (use with caution due to floating point
     * precision)
     */
    public Money(double amount, String currency) {
        this(BigDecimal.valueOf(amount), currency);
    }

    /**
     * Add another money value
     * 
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money add(Money other) {
        validateCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtract another money value
     * 
     * @throws IllegalArgumentException if currencies don't match or result would be
     *                                  negative
     */
    public Money subtract(Money other) {
        validateCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Subtraction would result in negative amount: " + this + " - " + other);
        }
        return new Money(result, this.currency);
    }

    /**
     * Multiply by a factor
     */
    public Money multiply(BigDecimal factor) {
        if (factor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Factor cannot be negative: " + factor);
        }
        return new Money(this.amount.multiply(factor), this.currency);
    }

    /**
     * Multiply by an integer quantity
     */
    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + quantity);
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }

    /**
     * Check if this money is zero
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if this money is greater than another
     */
    public boolean isGreaterThan(Money other) {
        validateCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Check if this money is less than another
     */
    public boolean isLessThan(Money other) {
        validateCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Static factory method for zero money
     */
    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * Static factory method for zero VND
     */
    public static Money zeroVND() {
        return new Money(BigDecimal.ZERO, "VND");
    }

    /**
     * Validate that currencies match for operations
     */
    private void validateCurrency(Money other) {
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
