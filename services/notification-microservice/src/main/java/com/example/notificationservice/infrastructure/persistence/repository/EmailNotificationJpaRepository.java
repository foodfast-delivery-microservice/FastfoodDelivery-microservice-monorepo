package com.example.notificationservice.infrastructure.persistence.repository;

import com.example.notificationservice.domain.valueobjects.EmailStatus;
import com.example.notificationservice.infrastructure.persistence.entity.EmailNotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for email notifications.
 */
@Repository
public interface EmailNotificationJpaRepository extends JpaRepository<EmailNotificationJpaEntity, Long> {

    List<EmailNotificationJpaEntity> findByStatus(EmailStatus status);

    List<EmailNotificationJpaEntity> findByStatusAndLastRetryAtBefore(EmailStatus status, Instant before);

    List<EmailNotificationJpaEntity> findByRecipient(String recipient);

    List<EmailNotificationJpaEntity> findByEventId(String eventId);
}
