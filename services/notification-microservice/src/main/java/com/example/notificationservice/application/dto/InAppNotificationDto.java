package com.example.notificationservice.application.dto;

import com.example.notificationservice.domain.valueobjects.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for InAppNotification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotificationDto {
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
}
