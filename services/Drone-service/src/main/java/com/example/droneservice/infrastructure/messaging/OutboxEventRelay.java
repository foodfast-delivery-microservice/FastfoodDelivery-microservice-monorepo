package com.example.droneservice.infrastructure.messaging;

import com.example.droneservice.domain.entities.OutboxEvent;
import com.example.droneservice.domain.valueobjects.EventStatus;
import com.example.droneservice.domain.repository.OutboxEventRepository;
import com.example.droneservice.infrastructure.config.RabbitMQConfig;
import com.example.droneservice.infrastructure.event.DeliveryCompletedEvent;
import com.example.droneservice.infrastructure.event.DroneAssignedEvent;
import com.example.droneservice.infrastructure.event.DroneStatusUpdateEvent;
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

    // Chạy định kỳ mỗi 5 giây
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void pollAndPublishEvents() {
        // 1. Tìm tất cả event có trạng thái NEW
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(EventStatus.NEW);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending drone events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // 2. Xác định routing key và payload object dựa trên loại event
                String routingKey;
                Object payloadToSend;

                if ("DroneAssigned".equals(event.getType())) {
                    routingKey = RabbitMQConfig.DRONE_ASSIGNED_ROUTING_KEY;
                    try {
                        payloadToSend = objectMapper.readValue(event.getPayload(), DroneAssignedEvent.class);
                    } catch (Exception ex) {
                        log.error("Failed to deserialize DroneAssigned payload for event id={}", event.getId(), ex);
                        payloadToSend = event.getPayload();
                    }
                } else if ("DeliveryCompleted".equals(event.getType())) {
                    routingKey = RabbitMQConfig.DELIVERY_COMPLETED_ROUTING_KEY;
                    try {
                        payloadToSend = objectMapper.readValue(event.getPayload(), DeliveryCompletedEvent.class);
                    } catch (Exception ex) {
                        log.error("Failed to deserialize DeliveryCompleted payload for event id={}", event.getId(), ex);
                        payloadToSend = event.getPayload();
                    }
                } else if ("DroneStatusUpdate".equals(event.getType())) {
                    routingKey = RabbitMQConfig.DRONE_STATUS_UPDATE_ROUTING_KEY;
                    try {
                        payloadToSend = objectMapper.readValue(event.getPayload(), DroneStatusUpdateEvent.class);
                    } catch (Exception ex) {
                        log.error("Failed to deserialize DroneStatusUpdate payload for event id={}", event.getId(), ex);
                        payloadToSend = event.getPayload();
                    }
                } else {
                    log.warn("Unknown event type: {}, skipping...", event.getType());
                    continue;
                }

                // 3. Gửi event lên RabbitMQ
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.DRONE_EXCHANGE,
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
