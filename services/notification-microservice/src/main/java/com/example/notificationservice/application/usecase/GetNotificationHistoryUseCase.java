package com.example.notificationservice.application.usecase;

import com.example.notificationservice.domain.entities.EmailNotification;
import com.example.notificationservice.domain.repository.EmailNotificationRepository;
import com.example.notificationservice.domain.valueobjects.EmailStatus;
import com.example.notificationservice.domain.valueobjects.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Use case for getting paginated and filtered history of email notifications (Admin only).
 */
@Service
@RequiredArgsConstructor
public class GetNotificationHistoryUseCase {

    private final EmailNotificationRepository repository;

    /**
     * Executes the filtered search of email notifications.
     *
     * @param status         email status (optional)
     * @param type           notification type (optional)
     * @param recipientEmail recipient email address (optional)
     * @param fromDate       start range of creation date (optional)
     * @param toDate         end range of creation date (optional)
     * @param pageable       pagination and sorting details
     * @return a page of EmailNotification domain entities
     */
    public Page<EmailNotification> execute(EmailStatus status, NotificationType type, String recipientEmail, Instant fromDate, Instant toDate, Pageable pageable) {
        return repository.findAll(status, type, recipientEmail, fromDate, toDate, pageable);
    }
}
