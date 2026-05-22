package com.example.notificationservice.application.usecase;

import com.example.notificationservice.domain.entities.InAppNotification;
import com.example.notificationservice.domain.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Use case for marking in-app notifications as read (individually or all at once).
 */
@Service
@RequiredArgsConstructor
public class MarkNotificationReadUseCase {

    private final InAppNotificationRepository repository;

    /**
     * Marks a single in-app notification as read.
     *
     * @param id     the notification ID
     * @param userId the user ID (used for ownership verification)
     * @throws IllegalArgumentException if the notification does not exist or does not belong to the user
     */
    public void execute(Long id, Long userId) {
        if (id == null) {
            throw new IllegalArgumentException("Notification ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        InAppNotification notification = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("In-app notification not found with ID: " + id));

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to modify notification for another user");
        }

        notification.markAsRead();
        repository.save(notification);
    }

    /**
     * Marks all unread in-app notifications for a user as read.
     *
     * @param userId the user ID
     */
    public void executeAll(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        repository.markAllAsRead(userId);
    }
}
