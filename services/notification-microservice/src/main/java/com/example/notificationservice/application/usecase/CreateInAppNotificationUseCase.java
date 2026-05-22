package com.example.notificationservice.application.usecase;

import com.example.notificationservice.domain.entities.InAppNotification;
import com.example.notificationservice.domain.repository.InAppNotificationRepository;
import com.example.notificationservice.domain.valueobjects.NotificationType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Use case for creating and persisting in-app notifications.
 */
@Service
@RequiredArgsConstructor
@Validated
public class CreateInAppNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateInAppNotificationUseCase.class);
    private final InAppNotificationRepository repository;

    /**
     * Creates and saves an in-app notification.
     *
     * @param userId      the user to receive the notification
     * @param title       notification title
     * @param message     notification message content
     * @param type        type of notification
     * @param referenceId reference ID (e.g. orderId, paymentId)
     * @return the saved InAppNotification entity
     */
    public InAppNotification execute(Long userId, String title, String message, NotificationType type, String referenceId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }

        log.info("Creating in-app notification: userId={}, title={}, type={}", userId, title, type);

        InAppNotification notification = InAppNotification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .build();

        return repository.save(notification);
    }
}
