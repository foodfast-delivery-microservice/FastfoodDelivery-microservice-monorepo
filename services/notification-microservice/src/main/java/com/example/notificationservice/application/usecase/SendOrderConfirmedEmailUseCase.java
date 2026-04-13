package com.example.notificationservice.application.usecase;

import com.example.notificationservice.application.dto.OrderConfirmedEventDto;
import com.example.notificationservice.application.dto.UserEmailResponse;
import com.example.notificationservice.domain.port.EmailSenderPort;
import com.example.notificationservice.domain.port.UserServicePort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Use case for sending order confirmed email notifications.
 */
@Service
@RequiredArgsConstructor
@Validated
public class SendOrderConfirmedEmailUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendOrderConfirmedEmailUseCase.class);

    private final UserServicePort userServicePort;
    private final EmailSenderPort emailSenderPort;

    /**
     * Handles order confirmed event and sends email notification.
     * @param event order confirmed event DTO (validated via Bean Validation)
     * @throws IllegalArgumentException if event is invalid or user email not found
     */
    public void handle(@Valid OrderConfirmedEventDto event) {
        if (event == null) {
            throw new IllegalArgumentException("Order confirmed event cannot be null");
        }

        if (event.getUserId() == null) {
            log.warn("Order confirmed event without userId, skipping email. event={}", event);
            throw new IllegalArgumentException("User ID cannot be null");
        }

        log.info("Processing order confirmed email: orderId={}, userId={}", 
                event.getOrderId(), event.getUserId());

        UserEmailResponse user = userServicePort.getUserEmailById(event.getUserId());
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("No email found for userId={}, skipping order confirmed email", event.getUserId());
            throw new IllegalArgumentException("User email not found for userId: " + event.getUserId());
        }

        emailSenderPort.sendOrderConfirmedEmail(event, user.getEmail());
        log.info("Order confirmed email sent successfully: userId={}, orderId={}", 
                event.getUserId(), event.getOrderId());
    }
}
