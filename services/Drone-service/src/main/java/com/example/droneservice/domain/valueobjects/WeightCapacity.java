package com.example.droneservice.domain.valueobjects;

import java.util.Objects;

/**
 * Value object representing drone weight capacity.
 * Encapsulates weight validation and capacity checks.
 */
public class WeightCapacity {
    private final double valueKg;
    private static final double MAX_CAPACITY_KG = 5.0;

    public WeightCapacity(double valueKg) {
        if (valueKg < 0) {
            throw new IllegalArgumentException("Weight capacity cannot be negative");
        }
        if (valueKg > MAX_CAPACITY_KG) {
            throw new IllegalArgumentException(
                    "Weight capacity cannot exceed " + MAX_CAPACITY_KG + " kg");
        }
        this.valueKg = valueKg;
    }

    /**
     * Check if this capacity can carry a given weight
     */
    public boolean canCarry(double weightKg) {
        return weightKg <= valueKg && weightKg >= 0;
    }

    public double getValue() {
        return valueKg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WeightCapacity that = (WeightCapacity) o;
        return Double.compare(that.valueKg, valueKg) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueKg);
    }

    @Override
    public String toString() {
        return valueKg + " kg";
    }
}
