package com.example.notificationservice.domain.entities;

import com.example.notificationservice.domain.valueobjects.NotificationType;

import java.time.Instant;

/**
 * Domain entity representing an in-app notification.
 */
public class InAppNotification {

    private Long id;
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private String referenceId;
    private String channel;
    private boolean isRead;
    private Instant createdAt;
    private Instant readAt;

    private InAppNotification() {
        // Private constructor for builder
    }

    public static InAppNotificationBuilder builder() {
        return new InAppNotificationBuilder();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getChannel() {
        return channel;
    }

    public boolean isRead() {
        return isRead;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = Instant.now();
    }

    public static class InAppNotificationBuilder {
        private Long id;
        private Long userId;
        private String title;
        private String message;
        private NotificationType type;
        private String referenceId;
        private String channel = "IN_APP";
        private boolean isRead = false;
        private Instant createdAt = Instant.now();
        private Instant readAt;

        public InAppNotificationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public InAppNotificationBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public InAppNotificationBuilder title(String title) {
            this.title = title;
            return this;
        }

        public InAppNotificationBuilder message(String message) {
            this.message = message;
            return this;
        }

        public InAppNotificationBuilder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public InAppNotificationBuilder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public InAppNotificationBuilder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public InAppNotificationBuilder isRead(boolean isRead) {
            this.isRead = isRead;
            return this;
        }

        public InAppNotificationBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public InAppNotificationBuilder readAt(Instant readAt) {
            this.readAt = readAt;
            return this;
        }

        public InAppNotification build() {
            InAppNotification notification = new InAppNotification();
            notification.id = this.id;
            notification.userId = this.userId;
            notification.title = this.title;
            notification.message = this.message;
            notification.type = this.type;
            notification.referenceId = this.referenceId;
            notification.channel = this.channel;
            notification.isRead = this.isRead;
            notification.createdAt = this.createdAt;
            notification.readAt = this.readAt;
            return notification;
        }
    }
}
