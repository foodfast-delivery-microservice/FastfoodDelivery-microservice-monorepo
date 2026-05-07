package com.example.notificationservice.domain.port;

import com.example.notificationservice.application.dto.SendGridWebhookEvent;

public interface WebhookEventPort {
    boolean isEventProcessed(String sgEventId);
    void recordEventProcessed(String sgEventId, String eventType, String email, Long userId, Long notificationId, String eventData);
}
