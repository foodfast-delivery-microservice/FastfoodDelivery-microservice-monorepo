package com.example.notificationservice.application.usecase;

import com.example.notificationservice.application.dto.SendGridWebhookEvent;
import com.example.notificationservice.domain.port.UserServicePort;
import com.example.notificationservice.domain.port.WebhookEventPort;
import com.example.notificationservice.domain.repository.EmailNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessBounceWebhookUseCase {

    private final WebhookEventPort webhookEventPort;
    private final UserServicePort userServicePort;
    private final EmailNotificationRepository emailNotificationRepository;

    @Transactional
    public void execute(SendGridWebhookEvent event) {
        if (event.getSgEventId() == null) {
            log.warn("Webhook event missing sgEventId, skipping");
            return;
        }

        // Check idempotency
        if (webhookEventPort.isEventProcessed(event.getSgEventId())) {
            log.debug("Webhook event {} already processed, skipping duplicate", event.getSgEventId());
            return;
        }

        try {
            Long userId = null;
            Long notificationId = null;

            if (event.getUserId() != null) {
                try {
                    userId = Long.parseLong(event.getUserId());
                } catch (NumberFormatException e) {
                    log.warn("Invalid userId in webhook event: {}", event.getUserId());
                }
            }

            if (event.getNotificationId() != null) {
                try {
                    notificationId = Long.parseLong(event.getNotificationId());
                } catch (NumberFormatException e) {
                    log.warn("Invalid notificationId in webhook event: {}", event.getNotificationId());
                }
            }

            // Process bounce event
            if ("bounce".equalsIgnoreCase(event.getEvent())) {
                processBounce(event, userId, notificationId);
            }

            // Record event as processed
            String eventData = String.format("event=%s, reason=%s, type=%s",
                    event.getEvent(), event.getReason(), event.getType());
            webhookEventPort.recordEventProcessed(
                    event.getSgEventId(),
                    event.getEvent(),
                    event.getEmail(),
                    userId,
                    notificationId,
                    eventData
            );

            log.info("Successfully processed webhook event: sgEventId={}, type={}, email={}",
                    event.getSgEventId(), event.getEvent(), event.getEmail());

        } catch (Exception e) {
            log.error("Error processing webhook event: sgEventId={}", event.getSgEventId(), e);
            throw e;
        }
    }

    private void processBounce(SendGridWebhookEvent event, Long userId, Long notificationId) {
        boolean isPermanent = "permanent".equalsIgnoreCase(event.getType());

        if (isPermanent && userId != null) {
            LocalDateTime bouncedAt = event.getTimestamp() != null
                    ? LocalDateTime.ofInstant(Instant.ofEpochSecond(event.getTimestamp()), ZoneId.systemDefault())
                    : LocalDateTime.now();

            boolean updated = userServicePort.updateEmailDeliverability(
                    userId,
                    true,
                    bouncedAt,
                    1
            );

            if (updated) {
                log.info("Marked user {} email as undeliverable due to permanent bounce", userId);
            } else {
                log.warn("Failed to update deliverability for user {}", userId);
            }
        } else if (!isPermanent && userId != null) {
            log.debug("Temporary bounce for user {}, not marking as undeliverable", userId);
        }
    }
}
