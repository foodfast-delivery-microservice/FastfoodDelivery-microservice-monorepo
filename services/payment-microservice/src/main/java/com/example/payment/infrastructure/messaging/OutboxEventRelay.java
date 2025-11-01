package com.example.payment.infrastructure.messaging;



import com.example.payment.domain.model.EventStatus;
import com.example.payment.domain.model.OutboxEvent;
import com.example.payment.domain.repository.OutboxEventRepository;
import com.example.payment.infrastructure.config.RabbitMQConfig;
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
                } else {
                    log.warn("Unknown event type: {}", event.getType());
                    continue;
                }

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.PAYMENT_EXCHANGE, // Gửi ra exchange của payment
                        routingKey,
                        event.getPayload()
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

