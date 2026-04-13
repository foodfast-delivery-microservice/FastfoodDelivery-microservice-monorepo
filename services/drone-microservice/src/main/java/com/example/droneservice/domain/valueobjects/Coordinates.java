package com.example.droneservice.domain.valueobjects;

import java.util.Objects;

import lombok.Getter;

/**
 * Value object representing geographic coordinates.
 * Encapsulates latitude and longitude with validation.
 */
@Getter
public class Coordinates {
    private final double latitude;
    private final double longitude;

    public Coordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Calculate distance to another coordinate using Haversine formula
     * 
     * @param other Target coordinates
     * @return Distance in kilometers
     */
    public double distanceTo(Coordinates other) {
        final int EARTH_RADIUS_KM = 6371;

        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLon = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                        * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculate next position moving towards target at given speed
     * 
     * @param target          Target coordinates
     * @param speedKmh        Speed in km/h
     * @param intervalSeconds Time interval in seconds
     * @return New coordinates after movement
     */
    public Coordinates moveTowards(Coordinates target, double speedKmh, int intervalSeconds) {
        double distanceKm = speedKmh * (intervalSeconds / 3600.0);
        double totalDistance = this.distanceTo(target);

        if (totalDistance <= distanceKm) {
            return target; // Reached or passed target
        }

        double fraction = distanceKm / totalDistance;

        // Linear interpolation for small distances (good enough approximation)
        double newLat = this.latitude + (target.latitude - this.latitude) * fraction;
        double newLon = this.longitude + (target.longitude - this.longitude) * fraction;

        return new Coordinates(newLat, newLon);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Coordinates that = (Coordinates) o;
        return Double.compare(that.latitude, latitude) == 0
                && Double.compare(that.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return "(" + latitude + ", " + longitude + ")";
    }
}
