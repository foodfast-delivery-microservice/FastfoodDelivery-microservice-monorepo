package com.example.notificationservice.domain.repository;

import com.example.notificationservice.domain.entities.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Domain port interface for InAppNotification repository.
 */
public interface InAppNotificationRepository {
    InAppNotification save(InAppNotification notification);
    Optional<InAppNotification> findById(Long id);
    Page<InAppNotification> findByUserId(Long userId, Pageable pageable);
    long countByUserIdAndIsReadFalse(Long userId);
    void markAllAsRead(Long userId);
}
