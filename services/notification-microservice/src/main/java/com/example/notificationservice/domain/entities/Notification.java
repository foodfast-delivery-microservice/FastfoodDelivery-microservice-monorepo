package com.example.notificationservice.domain.entities;

import com.example.notificationservice.domain.valueobjects.EmailAddress;
import com.example.notificationservice.domain.valueobjects.NotificationType;

import java.util.Map;

/**
 * Domain entity representing a notification.
 * Contains business logic for notification validation and processing.
 */
public class Notification {

    private NotificationType type;
    private EmailAddress recipient;
    private String template;
    private Map<String, Object> data;
    private String subject;

    private Notification() {
        // Private constructor for builder pattern
    }

    public static NotificationBuilder builder() {
        return new NotificationBuilder();
    }

    public NotificationType getType() {
        return type;
    }

    public EmailAddress getRecipient() {
        return recipient;
    }

    public String getTemplate() {
        return template;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getSubject() {
        return subject;
    }

    /**
     * Validates that the notification can be sent.
     *
     * @throws IllegalStateException if notification is invalid
     */
    public void validate() {
        if (type == null) {
            throw new IllegalStateException("Notification type cannot be null");
        }
        if (recipient == null) {
            throw new IllegalStateException("Recipient email cannot be null");
        }
        if (template == null || template.isBlank()) {
            throw new IllegalStateException("Template cannot be null or blank");
        }
    }

    /**
     * Checks if the notification can be sent.
     *
     * @return true if all required fields are present
     */
    public boolean canSend() {
        try {
            validate();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Gets the subject for the email, using default if not provided.
     *
     * @return email subject
     */
    public String getEmailSubject() {
        if (subject != null && !subject.isBlank()) {
            return subject;
        }
        return getDefaultSubject();
    }

    private String getDefaultSubject() {
        return switch (type) {
            case USER_REGISTERED -> "Welcome to FastFood Delivery!";
            case EMAIL_VERIFICATION_OTP -> "Verify your email address";
            case ORDER_CONFIRMED -> {
                String orderCode = data != null && data.containsKey("orderCode")
                        ? String.valueOf(data.get("orderCode"))
                        : "";
                yield "Order Confirmation #" + orderCode;
            }
            case PAYMENT_SUCCESS -> "Payment Successful!";
            case PAYMENT_FAILED -> "Payment Required: Transaction Failed";
            case PAYMENT_REFUNDED -> "Refund Processed";
            default -> "Notification from FastFood Delivery";
        };
    }

    public static class NotificationBuilder {
        private NotificationType type;
        private EmailAddress recipient;
        private String template;
        private Map<String, Object> data;
        private String subject;

        public NotificationBuilder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public NotificationBuilder type(String type) {
            this.type = NotificationType.fromString(type);
            return this;
        }

        public NotificationBuilder recipient(EmailAddress recipient) {
            this.recipient = recipient;
            return this;
        }

        public NotificationBuilder recipient(String email) {
            this.recipient = EmailAddress.of(email);
            return this;
        }

        public NotificationBuilder template(String template) {
            this.template = template;
            return this;
        }

        public NotificationBuilder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public NotificationBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Notification build() {
            Notification notification = new Notification();
            notification.type = this.type;
            notification.recipient = this.recipient;
            notification.template = this.template;
            notification.data = this.data;
            notification.subject = this.subject;
            return notification;
        }
    }
}
