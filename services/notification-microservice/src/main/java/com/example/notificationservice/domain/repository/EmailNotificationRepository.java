package com.example.notificationservice.domain.repository;

import com.example.notificationservice.domain.entities.EmailNotification;
import com.example.notificationservice.domain.valueobjects.EmailStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for email notifications.
 * Defines contract for persistence operations.
 */
public interface EmailNotificationRepository {

    EmailNotification save(EmailNotification notification);

    Optional<EmailNotification> findById(Long id);

    List<EmailNotification> findByStatus(EmailStatus status);

    List<EmailNotification> findByStatusAndLastRetryAtBefore(EmailStatus status, Instant before);

    List<EmailNotification> findByRecipient(String recipient);

    List<EmailNotification> findByEventId(String eventId);
}
