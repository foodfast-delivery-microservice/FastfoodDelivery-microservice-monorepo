package com.example.notificationservice.application.usecase;

import com.example.notificationservice.domain.entities.InAppNotification;
import com.example.notificationservice.domain.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Use case for retrieving in-app notifications and unread counts for a user.
 */
@Service
@RequiredArgsConstructor
public class GetInAppNotificationsUseCase {

    private final InAppNotificationRepository repository;

    /**
     * Retrieves a page of in-app notifications for a user.
     *
     * @param userId   the user ID
     * @param pageable pagination details
     * @return a page of in-app notifications
     */
    public Page<InAppNotification> execute(Long userId, Pageable pageable) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return repository.findByUserId(userId, pageable);
    }

    /**
     * Counts the number of unread in-app notifications for a user.
     *
     * @param userId the user ID
     * @return the count of unread notifications
     */
    public long countUnread(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return repository.countByUserIdAndIsReadFalse(userId);
    }
}
