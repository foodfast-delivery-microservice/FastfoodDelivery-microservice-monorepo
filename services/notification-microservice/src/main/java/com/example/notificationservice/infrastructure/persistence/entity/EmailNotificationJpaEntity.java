package com.example.notificationservice.infrastructure.persistence.entity;

import com.example.notificationservice.domain.valueobjects.EmailStatus;
import com.example.notificationservice.domain.valueobjects.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity for email notifications persistence.
 * Maps to domain entity EmailNotification.
 */
@Entity
@Table(name = "email_notifications", indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_recipient", columnList = "recipient"),
        @Index(name = "idx_event_id", columnList = "event_id"),
        @Index(name = "idx_status_retry", columnList = "status, last_retry_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EmailStatus status;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, length = 100)
    private String template;

    @Builder.Default
    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant sentAt;

    private Instant lastRetryAt;

    @Column(length = 1000)
    private String errorMessage;

    @Column(length = 100)
    private String eventId;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }
}
