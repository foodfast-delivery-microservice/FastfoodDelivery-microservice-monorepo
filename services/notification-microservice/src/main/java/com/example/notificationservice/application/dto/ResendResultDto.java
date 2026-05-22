package com.example.notificationservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for manual email resend results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResendResultDto {
    private boolean success;
    private String message;
    private EmailNotificationDto newAttempt;
}
