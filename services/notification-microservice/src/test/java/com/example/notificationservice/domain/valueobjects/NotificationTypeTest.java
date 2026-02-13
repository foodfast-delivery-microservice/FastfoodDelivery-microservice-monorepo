package com.example.notificationservice.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTypeTest {

    @Test
    void fromStringShouldReturnCorrectType() {
        assertEquals(NotificationType.USER_REGISTERED, NotificationType.fromString("USER_REGISTERED"));
        assertEquals(NotificationType.PAYMENT_SUCCESS, NotificationType.fromString("PAYMENT_SUCCESS"));
        assertEquals(NotificationType.ORDER_CONFIRMED, NotificationType.fromString("ORDER_CONFIRMED"));
    }

    @Test
    void fromStringShouldBeCaseInsensitive() {
        assertEquals(NotificationType.PAYMENT_SUCCESS, NotificationType.fromString("payment_success"));
        assertEquals(NotificationType.PAYMENT_SUCCESS, NotificationType.fromString("Payment_Success"));
        assertEquals(NotificationType.PAYMENT_SUCCESS, NotificationType.fromString("PAYMENT_SUCCESS"));
    }

    @Test
    void fromStringShouldReturnGenericForNull() {
        assertEquals(NotificationType.GENERIC, NotificationType.fromString(null));
    }

    @Test
    void fromStringShouldReturnGenericForBlank() {
        assertEquals(NotificationType.GENERIC, NotificationType.fromString(""));
        assertEquals(NotificationType.GENERIC, NotificationType.fromString("   "));
    }

    @Test
    void fromStringShouldReturnGenericForInvalidType() {
        assertEquals(NotificationType.GENERIC, NotificationType.fromString("INVALID_TYPE"));
        assertEquals(NotificationType.GENERIC, NotificationType.fromString("unknown"));
    }
}
