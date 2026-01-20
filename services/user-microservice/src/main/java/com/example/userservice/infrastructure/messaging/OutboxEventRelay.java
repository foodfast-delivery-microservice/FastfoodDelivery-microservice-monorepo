package com.example.userservice.infrastructure.messaging;

import com.example.userservice.domain.entities.OutboxEvent;
import com.example.userservice.domain.valueobjects.EventStatus;
import com.example.userservice.domain.repository.OutboxEventRepository;
import com.example.userservice.infrastructure.messaging.event.MerchantActivatedEvent;
import com.example.userservice.infrastructure.messaging.event.MerchantDeactivatedEvent;
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
                        log.error("Failed to deserialize MerchantDeactivated payload for event id={}", event.getId(), ex);
                        payloadToSend = event.getPayload();
                    }
                } else {
                    log.warn("Unknown event type: {}, skipping...", event.getType());
                    continue;
                }

                // 3. Gửi event lên RabbitMQ
                rabbitTemplate.convertAndSend(
                        USER_EVENTS_EXCHANGE,
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
