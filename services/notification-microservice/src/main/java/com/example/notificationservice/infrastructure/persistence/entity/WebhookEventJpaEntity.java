package com.example.notificationservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_sg_event_id", columnList = "sg_event_id", unique = true),
    @Index(name = "idx_processed_at", columnList = "processed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEventJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sg_event_id", nullable = false, unique = true, length = 255)
    private String sgEventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }
}
