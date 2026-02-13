package com.example.notificationservice.domain.port;

import com.example.notificationservice.application.dto.OrderConfirmedEventDto;
import com.example.notificationservice.application.dto.PaymentEventDto;
import com.example.notificationservice.domain.entities.Notification;

public interface EmailSenderPort {

    void sendPaymentSuccessEmail(PaymentEventDto event, String email);

    void sendPaymentFailedEmail(PaymentEventDto event, String email);

    void sendPaymentRefundedEmail(PaymentEventDto event, String email);

    void sendOrderConfirmedEmail(OrderConfirmedEventDto event, String email);

    /**
     * Sends a generic notification email.
     * @param notification domain entity containing all notification details
     * @throws RuntimeException if email sending fails
     */
    void sendGenericNotification(Notification notification);
}

