package com.example.notificationservice.application.dto;

import com.example.notificationservice.domain.valueobjects.EmailStatus;
import com.example.notificationservice.domain.valueobjects.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for EmailNotification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationDto {
    private Long id;
    private Long userId;
    private EmailStatus status;
    private NotificationType type;
    private String recipient;
    private String subject;
    private String template;
    private Integer retryCount;
    private Instant createdAt;
    private Instant sentAt;
    private Instant lastRetryAt;
    private String errorMessage;
    private String eventId;
    private String payloadJson;
}
