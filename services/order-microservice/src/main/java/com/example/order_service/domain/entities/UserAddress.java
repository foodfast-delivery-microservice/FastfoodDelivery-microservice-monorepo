package com.example.order_service.domain.entities;

import com.example.order_service.domain.valueobjects.AddressSource;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pure domain entity representing a User's saved address.
 * Persisted address that has been normalized and optionally adjusted on the
 * map.
 * No JPA annotations - this is a pure business object.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {

    private Long id;
    private Long userId;

    // Address components
    private String street;
    private String provinceCode;
    private String provinceName;
    private String communeCode;
    private String communeName;
    private String districtName;
    private String fullAddress;
    private String note;

    // Coordinates
    private BigDecimal lat;
    private BigDecimal lng;

    // Address quality indicator
    @Builder.Default
    private AddressSource source = AddressSource.GEOCODE_ONLY;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== Business Logic Methods ====================

    /**
     * Update location coordinates
     * Business Rule: When location is updated, source should be updated accordingly
     */
    public void updateLocation(BigDecimal newLat, BigDecimal newLng, AddressSource newSource) {
        validateCoordinates(newLat, newLng);

        this.lat = newLat;
        this.lng = newLng;
        this.source = newSource != null ? newSource : AddressSource.GEOCODE_USER_ADJUST;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark address as driver-adjusted
     * Business Rule: Driver adjustments are the most reliable
     */
    public void markAsDriverAdjusted(BigDecimal newLat, BigDecimal newLng) {
        validateCoordinates(newLat, newLng);

        this.lat = newLat;
        this.lng = newLng;
        this.source = AddressSource.DRIVER_ADJUST;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if address has coordinates
     */
    public boolean hasCoordinates() {
        return lat != null && lng != null;
    }

    /**
     * Get address quality score
     * Business Rule: DRIVER_ADJUST > GEOCODE_USER_ADJUST > GEOCODE_ONLY
     */
    public int getQualityScore() {
        return switch (source) {
            case DRIVER_ADJUST -> 3; // Highest quality
            case GEOCODE_USER_ADJUST -> 2; // Medium quality
            case GEOCODE_ONLY -> 1; // Lowest quality
        };
    }

    /**
     * Validate the user address
     */
    public void validate() {
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        if (street == null || street.isBlank()) {
            throw new IllegalStateException("Street cannot be blank");
        }
        if (provinceCode == null || provinceCode.isBlank()) {
            throw new IllegalStateException("Province code cannot be blank");
        }
        if (communeCode == null || communeCode.isBlank()) {
            throw new IllegalStateException("Commune code cannot be blank");
        }
        if (fullAddress == null || fullAddress.isBlank()) {
            throw new IllegalStateException("Full address cannot be blank");
        }

        // Validate coordinates if provided
        if ((lat == null) != (lng == null)) {
            throw new IllegalStateException(
                    "Both latitude and longitude must be provided together, or both omitted");
        }

        if (lat != null) {
            validateCoordinates(lat, lng);
        }
    }

    /**
     * Validate coordinates
     */
    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }

        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 ||
                latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90: " + latitude);
        }

        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 ||
                longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180: " + longitude);
        }
    }
}
