package com.example.notificationservice.application.usecase;

import com.example.notificationservice.application.dto.PaymentEventDto;
import com.example.notificationservice.application.dto.UserEmailResponse;
import com.example.notificationservice.domain.port.EmailSenderPort;
import com.example.notificationservice.domain.port.OrderServicePort;
import com.example.notificationservice.domain.port.UserServicePort;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;

/**
 * Use case for sending payment refunded email notifications.
 */
@Service
@RequiredArgsConstructor
@Validated
public class SendPaymentRefundedEmailUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendPaymentRefundedEmailUseCase.class);

    private final OrderServicePort orderServicePort;
    private final UserServicePort userServicePort;
    private final EmailSenderPort emailSenderPort;

    /**
     * Handles payment refunded event and sends email notification.
     * @param paymentId payment ID (must be positive)
     * @param orderId order ID (must be positive)
     * @param reason refund reason
     * @throws IllegalArgumentException if parameters are invalid or user email not found
     */
    public void handle(@NotNull @Positive Long paymentId, 
                      @NotNull @Positive Long orderId, 
                      String reason) {
        if (paymentId == null || paymentId <= 0) {
            throw new IllegalArgumentException("Payment ID must be positive");
        }
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }

        log.info("Processing payment refunded event: paymentId={}, orderId={}, reason={}", 
                paymentId, orderId, reason);

        // 1. Fetch order details to get userId and amount
        var order = orderServicePort.getOrderById(orderId);
        if (order == null) {
            log.warn("Order {} not found, skipping refunded email", orderId);
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        if (order.getUserId() == null) {
            log.warn("Order {} has no userId, skipping refunded email", orderId);
            throw new IllegalArgumentException("Order has no user ID: " + orderId);
        }

        // 2. Fetch user email
        UserEmailResponse user = userServicePort.getUserEmailById(order.getUserId());
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("No email found for userId={}, skipping payment refunded email", order.getUserId());
            throw new IllegalArgumentException("User email not found for userId: " + order.getUserId());
        }

        // 3. Build PaymentEventDto
        PaymentEventDto eventDto = PaymentEventDto.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(order.getUserId())
                .amount(order.getGrandTotal()) // Use amount from order
                .paymentTime(Instant.now())
                .status("REFUNDED")
                .failureReason(reason) // Reuse failureReason field for refund reason
                .build();

        // 4. Send email
        emailSenderPort.sendPaymentRefundedEmail(eventDto, user.getEmail());
        log.info("Payment refunded email sent successfully: userId={}, orderId={}", 
                order.getUserId(), orderId);
    }
}
