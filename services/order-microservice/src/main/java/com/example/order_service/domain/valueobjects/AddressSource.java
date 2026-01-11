package com.example.order_service.domain.valueobjects;

/**
 * Value Object indicating how reliable/accurate a stored address is.
 * Used for UserAddress quality tracking.
 */
public enum AddressSource {
    /** Address location is only based on automatic geocoding result */
    GEOCODE_ONLY,

    /** User confirmed or adjusted the marker on the map */
    GEOCODE_USER_ADJUST,

    /** Shipper/driver adjusted the final drop-off location */
    DRIVER_ADJUST
}
