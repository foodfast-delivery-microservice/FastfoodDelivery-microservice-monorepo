package com.example.notificationservice.infrastructure.messaging.listener;

import com.example.notificationservice.application.dto.NotificationEvent;
import com.example.notificationservice.application.usecase.SendGenericNotificationUseCase;
import com.example.notificationservice.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for generic notification events from RabbitMQ.
 * Delegates to SendGenericNotificationUseCase for business logic.
 * <p>
 * Error Handling Strategy:
 * - Throws exception to trigger RabbitMQ retry mechanism and DLQ
 * - Invalid events (validation errors) are logged and exception is thrown
 * - This ensures failed notifications are not lost and can be retried
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final SendGenericNotificationUseCase sendGenericNotificationUseCase;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(NotificationEvent event) {
        log.info("Received notification event: type={}, recipient={}, template={}",
                event.getEventType(), event.getRecipient(), event.getTemplate());

        try {
            sendGenericNotificationUseCase.handle(event);
            log.info("Successfully processed notification event: {}", event.getEventType());
        } catch (IllegalArgumentException e) {
            // Validation errors - log and throw to send to DLQ for manual review
            log.error("Invalid notification event (sending to DLQ): {}", event, e);
            throw new RuntimeException("Invalid notification event", e);
        } catch (Exception e) {
            // Other errors - log and throw to trigger retry/DLQ
            log.error("Error processing notification event (sending to DLQ): {}", event, e);
            throw e;
        }
    }
}
