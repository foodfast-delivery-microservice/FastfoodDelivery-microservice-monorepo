package com.example.notificationservice.application.usecase;

import com.example.notificationservice.application.dto.NotificationEvent;
import com.example.notificationservice.domain.entities.Notification;
import com.example.notificationservice.domain.port.EmailSenderPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Use case for sending generic notifications.
 * Converts NotificationEvent DTO to domain entity and delegates to EmailSenderPort.
 */
@Service
@RequiredArgsConstructor
@Validated
public class SendGenericNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendGenericNotificationUseCase.class);

    private final EmailSenderPort emailSenderPort;

    /**
     * Handles a generic notification event.
     *
     * @param event notification event DTO (validated via Bean Validation)
     * @throws IllegalArgumentException if event is invalid or domain validation fails
     * @throws RuntimeException         if email sending fails
     */
    public void handle(@Valid NotificationEvent event) {
        if (event == null) {
            log.warn("Received null notification event, skipping");
            throw new IllegalArgumentException("Notification event cannot be null");
        }
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            throw new IllegalArgumentException("Event type cannot be blank");
        }
        if (event.getRecipient() == null || event.getRecipient().isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be blank");
        }
        if (event.getTemplate() == null || event.getTemplate().isBlank()) {
            throw new IllegalArgumentException("Template cannot be blank");
        }

        log.info("Processing generic notification: type={}, recipient={}, template={}",
                event.getEventType(), event.getRecipient(), event.getTemplate());

        try {
            // Convert DTO to domain entity
            Notification notification = Notification.builder()
                    .type(event.getEventType())
                    .recipient(event.getRecipient())
                    .userId(event.getUserId())
                    .template(event.getTemplate())
                    .data(event.getData())
                    .build();

            // Validate notification (domain validation)
            notification.validate();

            // Send email
            emailSenderPort.sendGenericNotification(notification);

            log.info("Successfully processed generic notification: type={}, recipient={}",
                    event.getEventType(), event.getRecipient());

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Invalid notification event: {}", event, e);
            throw new IllegalArgumentException("Invalid notification event: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error processing generic notification: {}", event, e);
            throw new RuntimeException("Failed to send notification email", e);
        }
    }
}
