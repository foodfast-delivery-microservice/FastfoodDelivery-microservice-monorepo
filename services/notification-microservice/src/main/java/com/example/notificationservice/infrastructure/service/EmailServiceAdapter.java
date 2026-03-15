package com.example.notificationservice.infrastructure.service;

import com.example.notificationservice.application.dto.OrderConfirmedEventDto;
import com.example.notificationservice.application.dto.PaymentEventDto;
import com.example.notificationservice.domain.entities.EmailNotification;
import com.example.notificationservice.domain.entities.Notification;
import com.example.notificationservice.domain.port.EmailSenderPort;
import com.example.notificationservice.domain.repository.EmailNotificationRepository;
import com.example.notificationservice.domain.valueobjects.EmailStatus;
import com.example.notificationservice.infrastructure.template.EmailTemplateEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final EmailTemplateEngine templateEngine;
    private final EmailNotificationRepository emailNotificationRepository;
    private final MeterRegistry meterRegistry;
    private final EmailSubjectService emailSubjectService;
    private final ObjectMapper objectMapper;

    @Value("${spring.mail.from:noreply@fastfooddelivery.com}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public void sendPaymentSuccessEmail(PaymentEventDto event, String email) {
        log.info("Sending payment success email to: {} for orderId: {}", email, event.getOrderId());
        Map<String, Object> templateVariables = buildTemplateVariables(event, true);
        String subject = emailSubjectService.getPaymentSuccessSubject(event.getOrderId());
        String eventId = "payment-" + event.getPaymentId() + "-order-" + event.getOrderId();
        sendEmail(email, subject, "payment-success", templateVariables, "payment success",
                com.example.notificationservice.domain.valueobjects.NotificationType.PAYMENT_SUCCESS, eventId);
    }

    @Override
    public void sendPaymentFailedEmail(PaymentEventDto event, String email) {
        log.info("Sending payment failed email to: {} for orderId: {}", email, event.getOrderId());
        Map<String, Object> templateVariables = buildTemplateVariables(event, false);
        String subject = emailSubjectService.getPaymentFailedSubject(event.getOrderId());
        String eventId = "payment-" + event.getPaymentId() + "-order-" + event.getOrderId();
        sendEmail(email, subject, "payment-failure", templateVariables, "payment failed",
                com.example.notificationservice.domain.valueobjects.NotificationType.PAYMENT_FAILED, eventId);
    }

    @Override
    public void sendPaymentRefundedEmail(PaymentEventDto event, String email) {
        log.info("Sending payment refunded email to: {} for orderId: {}", email, event.getOrderId());
        Map<String, Object> templateVariables = buildRefundedTemplateVariables(event);
        String subject = emailSubjectService.getPaymentRefundedSubject(event.getOrderId());
        String eventId = "payment-" + event.getPaymentId() + "-order-" + event.getOrderId();
        sendEmail(email, subject, "payment-refunded", templateVariables, "payment refunded",
                com.example.notificationservice.domain.valueobjects.NotificationType.PAYMENT_REFUNDED, eventId);
    }

    private Map<String, Object> buildTemplateVariables(PaymentEventDto event, boolean isSuccess) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("paymentId", event.getPaymentId());
        variables.put("amount", formatAmount(event.getAmount()));
        variables.put("transactionId", event.getTransactionId() != null ? event.getTransactionId() : "N/A");
        variables.put("paymentTime", formatDateTime(event.getPaymentTime()));

        if (!isSuccess) {
            variables.put("failureReason", event.getFailureReason() != null ? event.getFailureReason() : "Không xác định");
        }

        return variables;
    }

    private Map<String, Object> buildRefundedTemplateVariables(PaymentEventDto event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("paymentId", event.getPaymentId());
        variables.put("amount", formatAmount(event.getAmount()));
        variables.put("refundReason", event.getFailureReason() != null ? event.getFailureReason() : "Không xác định");
        variables.put("refundTime", formatDateTime(event.getPaymentTime()));
        return variables;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        return String.format("%,d VNĐ", amount.longValue());
    }

    @Override
    public void sendOrderConfirmedEmail(OrderConfirmedEventDto event, String email) {
        log.info("Sending order confirmed email to: {} for orderId: {}", email, event.getOrderId());
        Map<String, Object> templateVariables = buildOrderConfirmedTemplateVariables(event);
        String orderCode = event.getOrderCode() != null ? event.getOrderCode() : String.valueOf(event.getOrderId());
        String subject = emailSubjectService.getOrderConfirmedSubject(orderCode);
        String eventId = "order-" + event.getOrderId();
        sendEmail(email, subject, "order-confirmed", templateVariables, "order confirmed",
                com.example.notificationservice.domain.valueobjects.NotificationType.ORDER_CONFIRMED, eventId);
    }

    private Map<String, Object> buildOrderConfirmedTemplateVariables(OrderConfirmedEventDto event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("orderCode", event.getOrderCode() != null ? event.getOrderCode() : String.valueOf(event.getOrderId()));
        variables.put("amount", formatAmount(event.getAmount()));
        variables.put("timestamp", event.getTimestamp() != null ? event.getTimestamp() : "N/A");
        return variables;
    }

    private String formatDateTime(Instant instant) {
        if (instant == null) {
            return "N/A";
        }
        return DATE_TIME_FORMATTER.format(instant);
    }

    @Override
    public void sendGenericNotification(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification cannot be null");
        }

        // Validate notification before sending
        if (!notification.canSend()) {
            throw new IllegalStateException("Notification is invalid and cannot be sent");
        }

        log.info("Sending generic notification email: type={}, recipient={}, template={}",
                notification.getType(), notification.getRecipient(), notification.getTemplate());

        // Create email notification record
        String payloadJson = null;
        if (notification.getData() != null) {
            try {
                payloadJson = objectMapper.writeValueAsString(notification.getData());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize notification data for persistence, continuing without payload. type={}, recipient={}",
                        notification.getType(), notification.getRecipient(), e);
            }
        }

        EmailNotification emailRecord = EmailNotification.builder()
                .type(notification.getType())
                .recipient(notification.getRecipient())
                .subject(notification.getEmailSubject())
                .template(notification.getTemplate())
                .status(EmailStatus.PENDING)
                .eventId(extractEventId(notification))
                .payloadJson(payloadJson)
                .build();
        emailRecord = emailNotificationRepository.save(emailRecord);

        try {
            Map<String, Object> templateVariables = notification.getData() != null 
                    ? new HashMap<>(notification.getData()) 
                    : new HashMap<>();
            
            // Send email (without rate limiter here as it's already persisted)
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(notification.getRecipient().getValue());
            helper.setSubject(notification.getEmailSubject());

            String htmlContent = templateEngine.render(notification.getTemplate(), templateVariables);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            // Update status
            emailRecord.markAsSent();
            emailNotificationRepository.save(emailRecord);

            // Metrics
            incrementEmailCounter("sent", notification.getType().name());

            log.info("Generic notification email sent successfully: type={}, recipient={}",
                    notification.getType(), notification.getRecipient());

        } catch (Exception e) {
            handleEmailFailure(emailRecord, e, "generic notification", 
                    notification.getRecipient().getValue(), notification.getType());
            throw new RuntimeException("Failed to send notification email", e);
        }
    }

    private String extractEventId(Notification notification) {
        if (notification.getData() != null) {
            Object orderId = notification.getData().get("orderId");
            Object paymentId = notification.getData().get("paymentId");
            if (orderId != null) {
                return "order-" + orderId;
            }
            if (paymentId != null) {
                return "payment-" + paymentId;
            }
        }
        return null;
    }

    /**
     * Common method to send email using template.
     * Reduces code duplication across all email sending methods.
     * Includes persistence, rate limiting, and metrics.
     *
     * @param to recipient email address
     * @param subject email subject
     * @param templateName template name (without .html extension)
     * @param templateVariables variables for template rendering
     * @param emailType description of email type for logging
     * @param notificationType notification type for persistence
     * @param eventId event ID for tracking (paymentId, orderId, etc.)
     * @throws RuntimeException if email sending fails
     */
    @RateLimiter(name = "emailRateLimiter")
    private void sendEmail(String to, String subject, String templateName, 
                           Map<String, Object> templateVariables, String emailType,
                           com.example.notificationservice.domain.valueobjects.NotificationType notificationType,
                           String eventId) {
        EmailNotification emailRecord = null;
        try {
            // Create email notification record
            emailRecord = EmailNotification.builder()
                    .type(notificationType)
                    .recipient(to)
                    .subject(subject)
                    .template(templateName)
                    .status(EmailStatus.PENDING)
                    .eventId(eventId)
                    .build();
            emailRecord = emailNotificationRepository.save(emailRecord);

            // Send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            String htmlContent = templateEngine.render(templateName, templateVariables);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            // Update status to SENT
            emailRecord.markAsSent();
            emailNotificationRepository.save(emailRecord);

            // Metrics
            incrementEmailCounter("sent", notificationType.name());

            log.info("{} email sent successfully to: {}", emailType, to);

        } catch (MessagingException e) {
            handleEmailFailure(emailRecord, e, emailType, to, notificationType);
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            handleEmailFailure(emailRecord, e, emailType, to, notificationType);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Overloaded method for backward compatibility (without persistence).
     * Used by generic notification which already handles persistence.
     */
    private void sendEmail(String to, String subject, String templateName, 
                           Map<String, Object> templateVariables, String emailType) {
        sendEmail(to, subject, templateName, templateVariables, emailType,
                com.example.notificationservice.domain.valueobjects.NotificationType.GENERIC, null);
    }

    private void handleEmailFailure(EmailNotification emailRecord, Exception e, 
                                   String emailType, String to,
                                   com.example.notificationservice.domain.valueobjects.NotificationType notificationType) {
        log.error("Failed to send {} email to: {}", emailType, to, e);

        String safeMessage = sanitizeErrorMessage(e);

        if (emailRecord != null) {
            emailRecord.markAsFailed(safeMessage);
            emailNotificationRepository.save(emailRecord);
        }

        // Metrics
        incrementEmailCounter("failed", notificationType.name());
    }

    private String sanitizeErrorMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return "Unexpected error";
        }
        String msg = e.getMessage().trim();
        int maxLength = 255;
        return msg.length() > maxLength ? msg.substring(0, maxLength - 3) + "..." : msg;
    }

    private void incrementEmailCounter(String status, String type) {
        Counter.builder("email.notifications")
                .tag("status", status)
                .tag("type", type)
                .register(meterRegistry)
                .increment();
    }
}
