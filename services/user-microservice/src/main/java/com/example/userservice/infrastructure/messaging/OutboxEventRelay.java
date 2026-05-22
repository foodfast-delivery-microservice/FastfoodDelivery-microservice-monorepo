package com.example.userservice.infrastructure.messaging;

import com.example.userservice.domain.entities.OutboxEvent;
import com.example.userservice.domain.valueobjects.EventStatus;
import com.example.userservice.domain.repository.OutboxEventRepository;
import com.example.userservice.infrastructure.messaging.event.MerchantActivatedEvent;
import com.example.userservice.infrastructure.messaging.event.MerchantDeactivatedEvent;
import com.example.userservice.application.DTOs.event.EmailVerificationOtpRequestedEvent;
import com.example.userservice.application.DTOs.event.UserUpdatedEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${app.otp.email.ttl-minutes:10}")
    private long otpTtlMinutes;

    // Tên Exchange (sàn giao dịch)
    public static final String USER_EVENTS_EXCHANGE = "user.events";
    // Tên routing keys (định tuyến)
    public static final String USER_UPDATED_ROUTING_KEY = "user.updated";
    public static final String MERCHANT_ACTIVATED_ROUTING_KEY = "merchant.activated";
    public static final String MERCHANT_DEACTIVATED_ROUTING_KEY = "merchant.deactivated";

    // Chạy định kỳ mỗi 5 giây
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void pollAndPublishEvents() {
        // 1. Tìm tất cả event có trạng thái NEW
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(EventStatus.NEW);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // 2. Xác định routing key và payload object dựa trên loại event
                String exchange = USER_EVENTS_EXCHANGE;
                String routingKey;
                Object payloadToSend;

                if ("UserUpdated".equals(event.getType())) {
                    routingKey = USER_UPDATED_ROUTING_KEY;
                    try {
                        payloadToSend = objectMapper.readValue(event.getPayload(), UserUpdatedEventDTO.class);
                    } catch (Exception ex) {
                        log.error("Failed to deserialize UserUpdated payload for event id={}", event.getId(), ex);
                        payloadToSend = event.getPayload();
                    }
                } else if ("MerchantActivated".equals(event.getType())) {
                    routingKey = MERCHANT_ACTIVATED_ROUTING_KEY;
                    try {
                        payloadToSend = objectMapper.readValue(event.getPayload(), MerchantActivatedEvent.class);
                    } catch (Exception ex) {
                        log.error("Failed to deserialize MerchantActivated payload for event id={}", event.getId(), ex);
                        payloadToSend = event.getPayload();
                    }
                } else if ("MerchantDeactivated".equals(event.getType())) {
                    routingKey = MERCHANT_DEACTIVATED_ROUTING_KEY;
                    try {
                        payloadToSend = objectMapper.readValue(event.getPayload(), MerchantDeactivatedEvent.class);
                    } catch (Exception ex) {
                        log.error("Failed to deserialize MerchantDeactivated payload for event id={}", event.getId(),
                                ex);
                        payloadToSend = event.getPayload();
                    }
                } else if ("UserRegistered".equals(event.getType())) {
                    // Send to Notification Service via Notification Exchange
                    exchange = "notification.exchange";
                    routingKey = "notification.user.registered";

                    try {
                        com.example.userservice.application.DTOs.event.UserRegisteredEvent userEvent = objectMapper
                                .readValue(event.getPayload(),
                                        com.example.userservice.application.DTOs.event.UserRegisteredEvent.class);

                        // Map to NotificationEvent structure (as Map or specific DTO class if shared)
                        // Here we construct a compatible object structure (or Map) to match
                        // NotificationEvent in Consumer
                        java.util.Map<String, Object> notificationData = new java.util.HashMap<>();
                        notificationData.put("name", userEvent.getFullName());
                        notificationData.put("username", userEvent.getUsername());
                        notificationData.put("loginUrl", "http://localhost:5173/login"); // Should be externalized

                        // Create Notification Payload
                        java.util.Map<String, Object> notificationEvent = new java.util.HashMap<>();
                        notificationEvent.put("eventType", "USER_REGISTERED");
                        notificationEvent.put("recipient", userEvent.getEmail());
                        notificationEvent.put("userId", userEvent.getUserId());
                        notificationEvent.put("template", "welcome-email");
                        notificationEvent.put("data", notificationData);

                        payloadToSend = notificationEvent;

                    } catch (Exception ex) {
                        log.error("Failed to deserialize/map UserRegistered payload for event id={}", event.getId(),
                                ex);
                        // If mapping fails, maybe skip or strip metadata?
                        // For now we assume payload is valid JSON of UserRegisteredEvent
                        payloadToSend = event.getPayload();
                    }
                } else if ("EmailVerificationOtpRequested".equals(event.getType())) {
                    exchange = "notification.exchange";
                    routingKey = "notification.email.otp.requested";

                    try {
                        EmailVerificationOtpRequestedEvent otpEvent = objectMapper.readValue(
                                event.getPayload(),
                                EmailVerificationOtpRequestedEvent.class
                        );

                        java.util.Map<String, Object> data = new java.util.HashMap<>();
                        data.put("otpCode", otpEvent.getOtpCode());
                        data.put("type", otpEvent.getType());
                        data.put("expiresMinutes", otpTtlMinutes);

                        java.util.Map<String, Object> notificationEvent = new java.util.HashMap<>();
                        notificationEvent.put("eventType", "EMAIL_VERIFICATION_OTP");
                        notificationEvent.put("recipient", otpEvent.getEmail());
                        notificationEvent.put("userId", otpEvent.getUserId());
                        notificationEvent.put("template", "email-verification-otp");
                        notificationEvent.put("data", data);

                        payloadToSend = notificationEvent;
                    } catch (Exception ex) {
                        log.error("Failed to deserialize/map EmailVerificationOtpRequested payload for event id={}", event.getId(), ex);
                        payloadToSend = event.getPayload();
                    }
                } else if ("UserForgotPasswordEvent".equals(event.getType())) {
                    exchange = "notification.exchange";
                    routingKey = "notification.user.forgot.password";

                    try {
                        com.example.userservice.application.DTOs.event.UserForgotPasswordEvent forgotPwdEvent = objectMapper.readValue(
                                event.getPayload(),
                                com.example.userservice.application.DTOs.event.UserForgotPasswordEvent.class
                        );

                        java.util.Map<String, Object> data = new java.util.HashMap<>();
                        // Make sure your frontend has a route like /reset-password?token=...
                        data.put("resetLink", "http://localhost:5173/reset-password?token=" + forgotPwdEvent.getResetToken());

                        java.util.Map<String, Object> notificationEvent = new java.util.HashMap<>();
                        notificationEvent.put("eventType", "USER_FORGOT_PASSWORD");
                        notificationEvent.put("recipient", forgotPwdEvent.getEmail());
                        notificationEvent.put("template", "forgot-password");
                        notificationEvent.put("data", data);

                        payloadToSend = notificationEvent;
                    } catch (Exception ex) {
                        log.error("Failed to deserialize/map UserForgotPasswordEvent payload for event id={}", event.getId(), ex);
                        payloadToSend = event.getPayload();
                    }
                } else {
                    log.warn("Unknown event type: {}, skipping...", event.getType());
                    continue;
                }

                // 3. Gửi event lên RabbitMQ
                rabbitTemplate.convertAndSend(
                        exchange,
                        routingKey,
                        payloadToSend);

                // 4. Gửi thành công, cập nhật trạng thái
                event.markAsProcessed();
                outboxEventRepository.save(event);

                log.info("Published event type: {}, aggregateId: {}", event.getType(), event.getAggregateId());

            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getId(), e);
                // Nếu lỗi, KHÔNG update status
                // Lần quét sau (5 giây nữa) nó sẽ được thử lại
            }
        }
    }
}
