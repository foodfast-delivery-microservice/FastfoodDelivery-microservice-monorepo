package com.example.order_service.domain.valueobjects;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a unique order code.
 * Enforces format validation and uniqueness semantics.
 */
@Getter
@EqualsAndHashCode
public class OrderCode {
    private static final Pattern VALID_FORMAT = Pattern.compile("^ORD-[A-Z0-9-]{8,32}$");
    private static final int MIN_LENGTH = 12; // "ORD-" + 8 chars
    private static final int MAX_LENGTH = 36; // "ORD-" + 32 chars

    private final String value;

    /**
     * Create a new OrderCode
     * 
     * @param value The order code string
     * @throws IllegalArgumentException if order code is invalid
     */
    public OrderCode(String value) {
        Objects.requireNonNull(value, "Order code cannot be null");

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Order code cannot be empty");
        }

        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Order code length must be between %d and %d characters: %s",
                            MIN_LENGTH, MAX_LENGTH, trimmed));
        }

        if (!VALID_FORMAT.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Order code must follow format 'ORD-[A-Z0-9]{8,32}': " + trimmed);
        }

        this.value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
