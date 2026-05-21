package com.example.notificationservice.application.usecase;

import com.example.notificationservice.domain.entities.EmailNotification;
import com.example.notificationservice.domain.port.EmailSenderPort;
import com.example.notificationservice.domain.repository.EmailNotificationRepository;
import com.example.notificationservice.domain.valueobjects.EmailStatus;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Use case for resending a failed email notification (Admin only).
 * Implements audit cloning, idempotency checking, and rate limiting.
 */
@Service
@RequiredArgsConstructor
public class ResendFailedEmailUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResendFailedEmailUseCase.class);

    private final EmailNotificationRepository repository;
    private final EmailSenderPort emailSenderPort;

    /**
     * Executes the resend of a failed email notification.
     *
     * @param originalId the ID of the failed email notification
     * @return the newly created email notification attempt
     * @throws IllegalArgumentException if original ID not found
     * @throws IllegalStateException    if original notification is not FAILED, or if an active/successful resend already exists
     */
    @RateLimiter(name = "resendRateLimiter")
    public EmailNotification execute(Long originalId) {
        if (originalId == null) {
            throw new IllegalArgumentException("Original email notification ID cannot be null");
        }

        log.info("Processing resend request for email notification ID: {}", originalId);

        EmailNotification original = repository.findById(originalId)
                .orElseThrow(() -> new IllegalArgumentException("Email notification not found with ID: " + originalId));

        if (original.getStatus() != EmailStatus.FAILED) {
            throw new IllegalStateException("Only FAILED notifications can be resent. Current status: " + original.getStatus());
        }

        // Idempotency check: check if any other email with the same eventId is currently active or sent
        if (original.getEventId() != null) {
            List<EmailNotification> associated = repository.findByEventId(original.getEventId());
            boolean alreadyActiveOrSent = associated.stream()
                    .anyMatch(n -> !n.getId().equals(originalId) && 
                            (n.getStatus() == EmailStatus.SENT || 
                             n.getStatus() == EmailStatus.PENDING || 
                             n.getStatus() == EmailStatus.RETRYING));
            
            if (alreadyActiveOrSent) {
                log.warn("Resend rejected due to idempotency check. An active or sent email exists for eventId: {}", original.getEventId());
                throw new IllegalStateException("A successful or active send attempt already exists for eventId: " + original.getEventId());
            }
        }

        // Create new record for audit trail
        EmailNotification clone = EmailNotification.builder()
                .type(original.getType())
                .recipient(original.getRecipient())
                .subject(original.getSubject())
                .template(original.getTemplate())
                .status(EmailStatus.PENDING)
                .eventId(original.getEventId())
                .payloadJson(original.getPayloadJson())
                .userId(original.getUserId())
                .createdAt(Instant.now())
                .retryCount(0)
                .build();

        EmailNotification savedClone = repository.save(clone);

        try {
            // Trigger mail send
            emailSenderPort.sendEmailRecord(savedClone);
        } catch (Exception e) {
            log.error("Failed during resend execution for email ID: {}", savedClone.getId(), e);
            // Even if it fails, sendEmailRecord handles marking it failed internally
        }

        return repository.findById(savedClone.getId()).orElse(savedClone);
    }
}
