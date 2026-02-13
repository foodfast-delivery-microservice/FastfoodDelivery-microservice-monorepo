package com.example.notificationservice.domain.entities;

import com.example.notificationservice.domain.valueobjects.EmailAddress;
import com.example.notificationservice.domain.valueobjects.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void shouldCreateNotificationWithBuilder() {
        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .recipient("test@example.com")
                .template("payment-success")
                .build();

        assertNotNull(notification);
        assertEquals(NotificationType.PAYMENT_SUCCESS, notification.getType());
        assertEquals("test@example.com", notification.getRecipient().getValue());
        assertEquals("payment-success", notification.getTemplate());
    }

    @Test
    void shouldCreateNotificationWithStringType() {
        Notification notification = Notification.builder()
                .type("PAYMENT_SUCCESS")
                .recipient("test@example.com")
                .template("payment-success")
                .build();

        assertEquals(NotificationType.PAYMENT_SUCCESS, notification.getType());
    }

    @Test
    void shouldCreateNotificationWithStringRecipient() {
        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .recipient("test@example.com")
                .template("payment-success")
                .build();

        assertNotNull(notification.getRecipient());
        assertEquals("test@example.com", notification.getRecipient().getValue());
    }

    @Test
    void validateShouldPassForValidNotification() {
        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .recipient("test@example.com")
                .template("payment-success")
                .build();

        assertDoesNotThrow(notification::validate);
    }

    @Test
    void validateShouldThrowForNullType() {
        Notification notification = Notification.builder()
                .type((NotificationType) null)
                .recipient("test@example.com")
                .template("payment-success")
                .build();

        assertThrows(IllegalStateException.class, notification::validate);
    }

    @Test
    void validateShouldThrowForNullRecipient() {
        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .recipient((EmailAddress) null)
                .template("payment-success")
                .build();

        assertThrows(IllegalStateException.class, notification::validate);
    }

    @Test
    void validateShouldThrowForNullTemplate() {
        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .recipient("test@example.com")
                .template(null)
                .build();

        assertThrows(IllegalStateException.class, notification::validate);
    }

    @Test
    void validateShouldThrowForBlankTemplate() {
        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .recipient("test@example.com")
                .template("   ")
                .build();

        assertThrows(IllegalStateException.class, notification::validate);
    }

    @Test
    void canSendShouldReturnTrueForValidNotification() {
        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .recipient("test@example.com")
                .template("payment-success")
                .build();

        assertTrue(notification.canSend());
    }

    @Test
    void canSendShouldReturnFalseForInvalidNotification() {
        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .recipient((EmailAddress) null)
                .template("payment-success")
                .build();

        assertFalse(notification.canSend());
    }

    @Test
    void getEmailSubjectShouldReturnCustomSubjectIfProvided() {
        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .recipient("test@example.com")
                .template("payment-success")
                .subject("Custom Subject")
                .build();

        assertEquals("Custom Subject", notification.getEmailSubject());
    }

    @Test
    void getEmailSubjectShouldReturnDefaultSubjectForPaymentSuccess() {
        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .recipient("test@example.com")
                .template("payment-success")
                .build();

        assertEquals("Payment Successful!", notification.getEmailSubject());
    }

    @Test
    void getEmailSubjectShouldReturnDefaultSubjectForPaymentFailed() {
        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_FAILED)
                .recipient("test@example.com")
                .template("payment-failure")
                .build();

        assertEquals("Payment Required: Transaction Failed", notification.getEmailSubject());
    }

    @Test
    void getEmailSubjectShouldReturnDefaultSubjectForOrderConfirmed() {
        Map<String, Object> data = new HashMap<>();
        data.put("orderCode", "ORD123");

        Notification notification = Notification.builder()
                .type(NotificationType.ORDER_CONFIRMED)
                .recipient("test@example.com")
                .template("order-confirmed")
                .data(data)
                .build();

        assertEquals("Order Confirmation #ORD123", notification.getEmailSubject());
    }

    @Test
    void getEmailSubjectShouldReturnDefaultSubjectForUserRegistered() {
        Notification notification = Notification.builder()
                .type(NotificationType.USER_REGISTERED)
                .recipient("test@example.com")
                .template("user-registered")
                .build();

        assertEquals("Welcome to FastFood Delivery!", notification.getEmailSubject());
    }

    @Test
    void getEmailSubjectShouldReturnDefaultSubjectForGenericType() {
        Notification notification = Notification.builder()
                .type(NotificationType.GENERIC)
                .recipient("test@example.com")
                .template("generic")
                .build();

        assertEquals("Notification from FastFood Delivery", notification.getEmailSubject());
    }

    @Test
    void shouldStoreDataCorrectly() {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 123);

        Notification notification = Notification.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .recipient("test@example.com")
                .template("payment-success")
                .data(data)
                .build();

        assertNotNull(notification.getData());
        assertEquals("value1", notification.getData().get("key1"));
        assertEquals(123, notification.getData().get("key2"));
    }
}
