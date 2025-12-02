package com.example.order_service.domain.model;

/**
 * Indicates how reliable/accurate a stored address is.
 * GEOCODE_ONLY - only based on automatic geocoding result.
 * GEOCODE_USER_ADJUST - user confirmed/adjusted marker on the map.
 * DRIVER_ADJUST - shipper/driver adjusted the final drop-off location.
 */
public enum AddressSource {
    GEOCODE_ONLY,
    GEOCODE_USER_ADJUST,
    DRIVER_ADJUST
}



