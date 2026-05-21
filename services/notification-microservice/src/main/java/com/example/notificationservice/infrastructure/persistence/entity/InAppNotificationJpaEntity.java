package com.example.notificationservice.infrastructure.persistence.entity;

import com.example.notificationservice.domain.valueobjects.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity representing an in-app notification.
 */
@Entity
@Table(name = "in_app_notifications", indexes = {
        @Index(name = "idx_user_read", columnList = "user_id, is_read"),
        @Index(name = "idx_created", columnList = "created_at"),
        @Index(name = "idx_user_created", columnList = "user_id, created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String channel = "IN_APP";

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (isRead == null) {
            isRead = false;
        }
        if (channel == null) {
            channel = "IN_APP";
        }
    }
}
