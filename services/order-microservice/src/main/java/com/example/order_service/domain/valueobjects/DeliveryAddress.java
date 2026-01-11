package com.example.order_service.domain.valueobjects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object representing a delivery address for an order.
 * Contains all information needed for delivery.
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DeliveryAddress {

    private String receiverName;
    private String receiverPhone;
    private String addressLine1;
    private String ward;
    private String district;
    private String city;

    // Administrative codes/names for analytics
    private String provinceCode;
    private String provinceName;
    private String communeCode;
    private String communeName;
    private String normalizedDistrictName;

    // Coordinates
    private BigDecimal lat;
    private BigDecimal lng;

    /**
     * Get full formatted address string
     */
    public String getFullAddress() {
        if (addressLine1 == null || ward == null || district == null || city == null) {
            return "";
        }
        return String.format("%s, %s, %s, %s", addressLine1, ward, district, city);
    }

    /**
     * Check if address has coordinates
     */
    public boolean hasCoordinates() {
        return lat != null && lng != null;
    }

    /**
     * Validate the delivery address has all required fields
     */
    public void validate() {
        Objects.requireNonNull(receiverName, "Receiver name cannot be null");
        Objects.requireNonNull(receiverPhone, "Receiver phone cannot be null");
        Objects.requireNonNull(addressLine1, "Address line 1 cannot be null");
        Objects.requireNonNull(ward, "Ward cannot be null");
        Objects.requireNonNull(district, "District cannot be null");
        Objects.requireNonNull(city, "City cannot be null");

        if (receiverName.isBlank()) {
            throw new IllegalArgumentException("Receiver name cannot be blank");
        }
        if (receiverPhone.isBlank()) {
            throw new IllegalArgumentException("Receiver phone cannot be blank");
        }
        if (addressLine1.isBlank()) {
            throw new IllegalArgumentException("Address line 1 cannot be blank");
        }

        // Validate coordinates if provided
        if ((lat == null) != (lng == null)) {
            throw new IllegalArgumentException(
                    "Both latitude and longitude must be provided together, or both omitted");
        }

        if (lat != null) {
            validateLatitude(lat);
            validateLongitude(lng);
        }
    }

    /**
     * Validate latitude range
     */
    private void validateLatitude(BigDecimal latitude) {
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 ||
                latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90: " + latitude);
        }
    }

    /**
     * Validate longitude range
     */
    private void validateLongitude(BigDecimal longitude) {
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 ||
                longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180: " + longitude);
        }
    }

    @Override
    public String toString() {
        return String.format("DeliveryAddress{receiver=%s, phone=%s, address=%s}",
                receiverName, receiverPhone, getFullAddress());
    }
}
