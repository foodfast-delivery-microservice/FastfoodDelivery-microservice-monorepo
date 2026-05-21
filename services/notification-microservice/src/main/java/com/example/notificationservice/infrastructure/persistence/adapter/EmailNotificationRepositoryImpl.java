package com.example.notificationservice.infrastructure.persistence.adapter;

import com.example.notificationservice.domain.entities.EmailNotification;
import com.example.notificationservice.domain.repository.EmailNotificationRepository;
import com.example.notificationservice.domain.valueobjects.EmailAddress;
import com.example.notificationservice.domain.valueobjects.EmailStatus;
import com.example.notificationservice.infrastructure.persistence.entity.EmailNotificationJpaEntity;
import com.example.notificationservice.infrastructure.persistence.repository.EmailNotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing domain repository interface using JPA.
 */
@Component
@RequiredArgsConstructor
public class EmailNotificationRepositoryImpl implements EmailNotificationRepository {

    private final EmailNotificationJpaRepository jpaRepository;

    @Override
    public EmailNotification save(EmailNotification notification) {
        EmailNotificationJpaEntity entity = toJpaEntity(notification);
        EmailNotificationJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<EmailNotification> findById(Long id) {
        return jpaRepository.findById(id)
                .map(this::toDomainEntity);
    }

    @Override
    public List<EmailNotification> findByStatus(EmailStatus status) {
        return jpaRepository.findByStatus(status)
                .stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmailNotification> findByStatusAndLastRetryAtBefore(EmailStatus status, Instant before) {
        return jpaRepository.findByStatusAndLastRetryAtBefore(status, before)
                .stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmailNotification> findByRecipient(String recipient) {
        return jpaRepository.findByRecipient(recipient)
                .stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmailNotification> findByEventId(String eventId) {
        return jpaRepository.findByEventId(eventId)
                .stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public org.springframework.data.domain.Page<EmailNotification> findAll(
            EmailStatus status,
            com.example.notificationservice.domain.valueobjects.NotificationType type,
            String recipient,
            Instant fromDate,
            Instant toDate,
            org.springframework.data.domain.Pageable pageable) {
        return jpaRepository.findAllFiltered(status, type, recipient, fromDate, toDate, pageable)
                .map(this::toDomainEntity);
    }

    private EmailNotificationJpaEntity toJpaEntity(EmailNotification domain) {
        EmailNotificationJpaEntity entity = EmailNotificationJpaEntity.builder()
                .id(domain.getId())
                .status(domain.getStatus())
                .type(domain.getType())
                .recipient(domain.getRecipient().getValue())
                .subject(domain.getSubject())
                .template(domain.getTemplate())
                .retryCount(domain.getRetryCount())
                .createdAt(domain.getCreatedAt())
                .sentAt(domain.getSentAt())
                .lastRetryAt(domain.getLastRetryAt())
                .errorMessage(domain.getErrorMessage())
                .eventId(domain.getEventId())
                .payloadJson(domain.getPayloadJson())
                .userId(domain.getUserId())
                .build();
        return entity;
    }

    private EmailNotification toDomainEntity(EmailNotificationJpaEntity entity) {
        return EmailNotification.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .type(entity.getType())
                .recipient(entity.getRecipient())
                .subject(entity.getSubject())
                .template(entity.getTemplate())
                .retryCount(entity.getRetryCount())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .lastRetryAt(entity.getLastRetryAt())
                .errorMessage(entity.getErrorMessage())
                .eventId(entity.getEventId())
                .payloadJson(entity.getPayloadJson())
                .userId(entity.getUserId())
                .build();
    }
}
