package com.example.notificationservice.infrastructure.service;

import com.example.notificationservice.domain.entities.EmailNotification;
import com.example.notificationservice.domain.entities.Notification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.notificationservice.domain.repository.EmailNotificationRepository;
import com.example.notificationservice.domain.valueobjects.EmailStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for retrying failed email notifications with exponential backoff.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailRetryService {

    private final EmailNotificationRepository repository;
    private final EmailServiceAdapter emailServiceAdapter;
    private final ObjectMapper objectMapper;

    @Value("${notification.email.retry.max-retries:3}")
    private int maxRetries;

    @Value("${notification.email.retry.base-delay-seconds:60}")
    private int baseDelaySeconds;

    /**
     * Scheduled task to retry failed emails.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void retryFailedEmails() {
        log.info("Starting email retry job");

        List<EmailNotification> failedEmails = repository.findByStatus(EmailStatus.FAILED);
        List<EmailNotification> retryingEmails = repository.findByStatus(EmailStatus.RETRYING);

        int retriedCount = 0;
        for (EmailNotification notification : failedEmails) {
            if (shouldRetry(notification)) {
                retryEmail(notification);
                retriedCount++;
            }
        }

        for (EmailNotification notification : retryingEmails) {
            if (shouldRetry(notification)) {
                retryEmail(notification);
                retriedCount++;
            }
        }

        log.info("Email retry job completed. Retried {} emails", retriedCount);
    }

    private boolean shouldRetry(EmailNotification notification) {
        if (!notification.canRetry(maxRetries)) {
            log.debug("Email {} exceeded max retries, skipping", notification.getId());
            return false;
        }

        long delaySeconds = notification.calculateNextRetryDelay(baseDelaySeconds);
        Instant nextRetryTime = notification.getLastRetryAt() != null
                ? notification.getLastRetryAt().plusSeconds(delaySeconds)
                : notification.getCreatedAt().plusSeconds(delaySeconds);

        if (Instant.now().isBefore(nextRetryTime)) {
            log.debug("Email {} not ready for retry yet. Next retry at {}",
                    notification.getId(), nextRetryTime);
            return false;
        }

        return true;
    }

    private void retryEmail(EmailNotification notification) {
        try {
            log.info("Retrying email notification: id={}, recipient={}, attempt={}",
                    notification.getId(), notification.getRecipient(),
                    notification.getRetryCount() + 1);

            notification.markAsRetrying();
            repository.save(notification);

            // Reconstruct notification from stored data
            Map<String, Object> data = null;
            if (notification.getPayloadJson() != null) {
                try {
                    data = objectMapper.readValue(
                            notification.getPayloadJson(),
                            new TypeReference<Map<String, Object>>() {
                            }
                    );
                } catch (Exception ex) {
                    log.warn("Failed to deserialize payloadJson for email notification id={}, continuing without data",
                            notification.getId(), ex);
                }
            }

            Notification domainNotification = Notification.builder()
                    .type(notification.getType())
                    .recipient(notification.getRecipient())
                    .template(notification.getTemplate())
                    .subject(notification.getSubject())
                    .data(data)
                    .build();

            emailServiceAdapter.sendGenericNotification(domainNotification);

            log.info("Successfully retried email notification: id={}", notification.getId());

        } catch (Exception e) {
            log.error("Failed to retry email notification: id={}", notification.getId(), e);
            String safeMessage = e != null && e.getMessage() != null && !e.getMessage().isBlank()
                    ? (e.getMessage().length() > 255 ? e.getMessage().substring(0, 252) + "..." : e.getMessage())
                    : "Unexpected error";
            notification.markAsFailed(safeMessage);
            repository.save(notification);
        }
    }
}
