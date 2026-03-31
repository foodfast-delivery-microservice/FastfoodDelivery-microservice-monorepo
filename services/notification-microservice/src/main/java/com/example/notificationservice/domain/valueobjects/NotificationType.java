package com.example.notificationservice.domain.valueobjects;

/**
 * Enum representing different types of notifications.
 */
public enum NotificationType {
    USER_REGISTERED,
    EMAIL_VERIFICATION_OTP,
    ORDER_CONFIRMED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    ORDER_STATUS_CHANGED,
    GENERIC;

    public static NotificationType fromString(String type) {
        if (type == null || type.isBlank()) {
            return GENERIC;
        }
        try {
            return valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GENERIC;
        }
    }
}
