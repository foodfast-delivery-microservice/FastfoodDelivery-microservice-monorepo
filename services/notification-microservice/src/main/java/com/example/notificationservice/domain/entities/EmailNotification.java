package com.example.notificationservice.domain.entities;

import com.example.notificationservice.domain.valueobjects.EmailAddress;
import com.example.notificationservice.domain.valueobjects.EmailStatus;
import com.example.notificationservice.domain.valueobjects.NotificationType;

import java.time.Instant;

/**
 * Domain entity representing an email notification record.
 * Used for tracking email delivery status and retry logic.
 */
public class EmailNotification {

    private Long id;
    private NotificationType type;
    private EmailAddress recipient;
    private String subject;
    private String template;
    private EmailStatus status;
    private Integer retryCount;
    private Instant createdAt;
    private Instant sentAt;
    private Instant lastRetryAt;
    private String errorMessage;
    private String eventId; // Reference to original event (paymentId, orderId, etc.)
    private String payloadJson;

    private EmailNotification() {
        // Private constructor for builder
    }

    public static EmailNotificationBuilder builder() {
        return new EmailNotificationBuilder();
    }

    public Long getId() {
        return id;
    }

    public NotificationType getType() {
        return type;
    }

    public EmailAddress getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getTemplate() {
        return template;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getLastRetryAt() {
        return lastRetryAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getEventId() {
        return eventId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    /**
     * Marks email as sent successfully.
     */
    public void markAsSent() {
        this.status = EmailStatus.SENT;
        this.sentAt = Instant.now();
        this.errorMessage = null;
    }

    /**
     * Marks email as failed and increments retry count.
     * @param errorMessage error message
     */
    public void markAsFailed(String errorMessage) {
        this.status = EmailStatus.FAILED;
        this.errorMessage = errorMessage;
        this.lastRetryAt = Instant.now();
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        this.retryCount++;
    }

    /**
     * Marks email as retrying.
     */
    public void markAsRetrying() {
        this.status = EmailStatus.RETRYING;
        this.lastRetryAt = Instant.now();
    }

    /**
     * Checks if email can be retried based on max retry count.
     * @param maxRetries maximum number of retries allowed
     * @return true if can retry
     */
    public boolean canRetry(int maxRetries) {
        if (status == EmailStatus.SENT) {
            return false;
        }
        return retryCount == null || retryCount < maxRetries;
    }

    /**
     * Calculates next retry delay using exponential backoff.
     * @param baseDelaySeconds base delay in seconds
     * @return delay in seconds
     */
    public long calculateNextRetryDelay(int baseDelaySeconds) {
        if (retryCount == null || retryCount == 0) {
            return baseDelaySeconds;
        }
        // Exponential backoff: baseDelay * 2^retryCount
        return (long) (baseDelaySeconds * Math.pow(2, retryCount));
    }

    public static class EmailNotificationBuilder {
        private Long id;
        private NotificationType type;
        private EmailAddress recipient;
        private String subject;
        private String template;
        private EmailStatus status = EmailStatus.PENDING;
        private Integer retryCount = 0;
        private Instant createdAt = Instant.now();
        private Instant sentAt;
        private Instant lastRetryAt;
        private String errorMessage;
        private String eventId;
        private String payloadJson;

        public EmailNotificationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public EmailNotificationBuilder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public EmailNotificationBuilder type(String type) {
            this.type = NotificationType.fromString(type);
            return this;
        }

        public EmailNotificationBuilder recipient(EmailAddress recipient) {
            this.recipient = recipient;
            return this;
        }

        public EmailNotificationBuilder recipient(String email) {
            this.recipient = EmailAddress.of(email);
            return this;
        }

        public EmailNotificationBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public EmailNotificationBuilder template(String template) {
            this.template = template;
            return this;
        }

        public EmailNotificationBuilder status(EmailStatus status) {
            this.status = status;
            return this;
        }

        public EmailNotificationBuilder retryCount(Integer retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public EmailNotificationBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public EmailNotificationBuilder sentAt(Instant sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public EmailNotificationBuilder lastRetryAt(Instant lastRetryAt) {
            this.lastRetryAt = lastRetryAt;
            return this;
        }

        public EmailNotificationBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public EmailNotificationBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public EmailNotificationBuilder payloadJson(String payloadJson) {
            this.payloadJson = payloadJson;
            return this;
        }

        public EmailNotification build() {
            EmailNotification notification = new EmailNotification();
            notification.id = this.id;
            notification.type = this.type;
            notification.recipient = this.recipient;
            notification.subject = this.subject;
            notification.template = this.template;
            notification.status = this.status;
            notification.retryCount = this.retryCount;
            notification.createdAt = this.createdAt;
            notification.sentAt = this.sentAt;
            notification.lastRetryAt = this.lastRetryAt;
            notification.errorMessage = this.errorMessage;
            notification.eventId = this.eventId;
            notification.payloadJson = this.payloadJson;
            return notification;
        }
    }
}
