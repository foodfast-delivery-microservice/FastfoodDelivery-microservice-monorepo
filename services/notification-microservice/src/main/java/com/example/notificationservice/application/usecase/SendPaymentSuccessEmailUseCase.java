package com.example.notificationservice.application.usecase;

import com.example.notificationservice.application.dto.PaymentEventDto;
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
 * Use case for sending payment success email notifications.
 */
@Service
@RequiredArgsConstructor
@Validated
public class SendPaymentSuccessEmailUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendPaymentSuccessEmailUseCase.class);

    private final UserServicePort userServicePort;
    private final EmailSenderPort emailSenderPort;

    /**
     * Handles payment success event and sends email notification.
     * @param event payment event DTO (validated via Bean Validation)
     * @throws IllegalArgumentException if event is invalid or user email not found
     */
    public void handle(@Valid PaymentEventDto event) {
        if (event == null) {
            throw new IllegalArgumentException("Payment event cannot be null");
        }

        if (event.getUserId() == null) {
            log.warn("Payment success event without userId, skipping email. event={}", event);
            throw new IllegalArgumentException("User ID cannot be null");
        }

        log.info("Processing payment success email: paymentId={}, orderId={}, userId={}",
                event.getPaymentId(), event.getOrderId(), event.getUserId());

        UserEmailResponse user = userServicePort.getUserEmailById(event.getUserId());
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("No email found for userId={}, skipping payment success email", event.getUserId());
            throw new IllegalArgumentException("User email not found for userId: " + event.getUserId());
        }

        emailSenderPort.sendPaymentSuccessEmail(event, user.getEmail());
        log.info("Payment success email sent successfully: userId={}, orderId={}", 
                event.getUserId(), event.getOrderId());
    }
}

