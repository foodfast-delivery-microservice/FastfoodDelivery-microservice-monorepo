package com.example.droneservice.domain.valueobjects;

import java.util.Objects;

/**
 * Value object representing drone battery level.
 * Encapsulates battery validation and business rules.
 */
public class BatteryLevel {
    private final int value;
    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 100;
    private static final double BATTERY_CONSUMPTION_PER_KM = 2.0; // 2% per km

    public BatteryLevel(int value) {
        if (value < MIN_LEVEL || value > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "Battery level must be between " + MIN_LEVEL + " and " + MAX_LEVEL);
        }
        this.value = value;
    }

    /**
     * Check if battery level is above a given threshold
     */
    public boolean isAboveThreshold(int threshold) {
        return value >= threshold;
    }

    /**
     * Calculate if current battery can support a given distance
     * 
     * @param totalDistanceKm   Total distance in kilometers
     * @param reservePercentage Additional reserve percentage (default 10%)
     * @return true if battery is sufficient
     */
    public boolean canSupport(double totalDistanceKm, double reservePercentage) {
        double requiredBattery = totalDistanceKm * BATTERY_CONSUMPTION_PER_KM;
        double minimumNeeded = requiredBattery + reservePercentage;
        return value >= minimumNeeded;
    }

    /**
     * Calculate remaining battery after a distance
     */
    public BatteryLevel afterDistance(double distanceKm) {
        int consumed = (int) Math.ceil(distanceKm * BATTERY_CONSUMPTION_PER_KM);
        int remaining = Math.max(0, value - consumed);
        return new BatteryLevel(remaining);
    }

    /**
     * Calculate required battery for a given distance
     */
    public static double calculateRequired(double distanceKm) {
        return distanceKm * BATTERY_CONSUMPTION_PER_KM;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BatteryLevel that = (BatteryLevel) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value + "%";
    }
}
