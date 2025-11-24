package com.example.payment.infrastructure.messaging;

import com.example.payment.domain.model.EventStatus;
import com.example.payment.domain.model.OutboxEvent;
import com.example.payment.domain.repository.OutboxEventRepository;
import com.example.payment.infrastructure.config.RabbitMQConfig;
import com.example.payment.infrastructure.event.PaymentFailedEventPayload;
import com.example.payment.infrastructure.event.PaymentRefundedEvent;
import com.example.payment.infrastructure.event.PaymentSuccessEventPayload;
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

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void pollAndPublishEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(EventStatus.NEW);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[RELAY] Found {} pending payment events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Định nghĩa routing key dựa trên loại event
                String routingKey;
                if ("PAYMENT_SUCCESS".equals(event.getType())) {
                    routingKey = RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY;
                } else if ("PAYMENT_FAILED".equals(event.getType())) {
                    routingKey = RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY;
                } else if ("PAYMENT_REFUNDED".equals(event.getType())) {
                    routingKey = RabbitMQConfig.PAYMENT_REFUNDED_ROUTING_KEY;
                } else {
                    log.warn("Unknown event type: {}", event.getType());
                    continue;
                }

                // Parse JSON string payload to Object before sending
                // This prevents double-encoding when using Jackson2JsonMessageConverter
                Object payloadObject;
                try {
                    if ("PAYMENT_SUCCESS".equals(event.getType())) {
                        // Parse JSON string to PaymentSuccessEventPayload object
                        payloadObject = objectMapper.readValue(event.getPayload(), PaymentSuccessEventPayload.class);
                    } else if ("PAYMENT_FAILED".equals(event.getType())) {
                        // Parse JSON string to PaymentFailedEventPayload object
                        payloadObject = objectMapper.readValue(event.getPayload(), PaymentFailedEventPayload.class);
                    } else if ("PAYMENT_REFUNDED".equals(event.getType())) {
                        // Parse JSON string to PaymentRefundedEvent object
                        payloadObject = objectMapper.readValue(event.getPayload(), PaymentRefundedEvent.class);
                    } else {
                        // For other event types, send as-is (JSON string)
                        payloadObject = event.getPayload();
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse payload as object, sending as JSON string: {}", e.getMessage());
                    payloadObject = event.getPayload();
                }

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.PAYMENT_EXCHANGE, // Gửi ra exchange của payment
                        routingKey,
                        payloadObject
                );

                event.setStatus(EventStatus.PROCESSED);
                outboxEventRepository.save(event);

                log.info("[RELAY] Published event type: {}, aggregateId: {}", event.getType(), event.getAggregateId());

            } catch (Exception e) {
                log.error("[RELAY] Failed to publish event: {}", event.getId(), e);
            }
        }
    }
}

