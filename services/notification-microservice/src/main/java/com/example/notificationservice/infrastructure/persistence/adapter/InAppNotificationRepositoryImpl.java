package com.example.notificationservice.infrastructure.persistence.adapter;

import com.example.notificationservice.domain.entities.InAppNotification;
import com.example.notificationservice.domain.repository.InAppNotificationRepository;
import com.example.notificationservice.infrastructure.persistence.entity.InAppNotificationJpaEntity;
import com.example.notificationservice.infrastructure.persistence.repository.InAppNotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Adapter implementing the domain repository interface using JPA.
 */
@Component
@RequiredArgsConstructor
public class InAppNotificationRepositoryImpl implements InAppNotificationRepository {

    private final InAppNotificationJpaRepository jpaRepository;

    @Override
    @Transactional
    public InAppNotification save(InAppNotification notification) {
        InAppNotificationJpaEntity entity = toJpaEntity(notification);
        InAppNotificationJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InAppNotification> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InAppNotification> findByUserId(Long userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable).map(this::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserIdAndIsReadFalse(Long userId) {
        return jpaRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        jpaRepository.markAllAsRead(userId, Instant.now());
    }

    private InAppNotificationJpaEntity toJpaEntity(InAppNotification domain) {
        if (domain == null) {
            return null;
        }
        return InAppNotificationJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .title(domain.getTitle())
                .message(domain.getMessage())
                .type(domain.getType())
                .referenceId(domain.getReferenceId())
                .channel(domain.getChannel())
                .isRead(domain.isRead())
                .createdAt(domain.getCreatedAt())
                .readAt(domain.getReadAt())
                .build();
    }

    private InAppNotification toDomainEntity(InAppNotificationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return InAppNotification.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .referenceId(entity.getReferenceId())
                .channel(entity.getChannel())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }
}
