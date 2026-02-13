package com.example.droneservice.domain.valueobjects;

import java.util.Objects;

/**
 * Value object representing a drone's serial number.
 * Ensures immutability and validation of serial number constraints.
 */
public class SerialNumber {
    private final String value;
    private static final int MAX_LENGTH = 100;

    public SerialNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Serial number cannot be null or empty");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Serial number cannot exceed " + MAX_LENGTH + " characters");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SerialNumber that = (SerialNumber) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
