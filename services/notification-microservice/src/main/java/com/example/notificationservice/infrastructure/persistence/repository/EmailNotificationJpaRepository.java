package com.example.notificationservice.infrastructure.persistence.repository;

import com.example.notificationservice.domain.valueobjects.EmailStatus;
import com.example.notificationservice.infrastructure.persistence.entity.EmailNotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT n FROM EmailNotificationJpaEntity n WHERE " +
           "(:status IS NULL OR n.status = :status) AND " +
           "(:type IS NULL OR n.type = :type) AND " +
           "(:recipient IS NULL OR n.recipient = :recipient) AND " +
           "(:fromDate IS NULL OR n.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR n.createdAt <= :toDate)")
    Page<EmailNotificationJpaEntity> findAllFiltered(
            @Param("status") EmailStatus status,
            @Param("type") com.example.notificationservice.domain.valueobjects.NotificationType type,
            @Param("recipient") String recipient,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
