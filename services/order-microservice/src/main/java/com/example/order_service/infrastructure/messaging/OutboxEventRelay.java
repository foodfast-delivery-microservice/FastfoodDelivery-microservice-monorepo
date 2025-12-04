package com.example.order_service.infrastructure.messaging;

import com.example.order_service.domain.model.EventStatus;
import com.example.order_service.domain.model.OutboxEvent;
import com.example.order_service.domain.repository.OutboxEventRepository;
import com.example.order_service.infrastructure.event.OrderCreatedEventPayload;
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
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // Tên Exchange (sàn giao dịch)
    public static final String ORDER_EXCHANGE = "order_exchange";
    // Tên routing key (định tuyến)
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_REFUND_REQUEST_ROUTING_KEY = "order.refund.request";
    public static final String ORDER_REFUNDED_ROUTING_KEY = "order.refunded";
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";

    // Chạy định kỳ mỗi 5 giây
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void pollAndPublishEvents() {
        // 1. Tìm tất cả event có trạng thái NEW (giống EventStatus của bạn)
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(EventStatus.NEW);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // 2. Xác định routing key dựa trên loại event
                String routingKey;
                if ("OrderCreated".equals(event.getType())) {
                    routingKey = ORDER_CREATED_ROUTING_KEY;
                } else if ("OrderRefundRequest".equals(event.getType())) {
                    routingKey = ORDER_REFUND_REQUEST_ROUTING_KEY;
                } else if ("OrderRefunded".equals(event.getType())) {
                    routingKey = ORDER_REFUNDED_ROUTING_KEY;
                } else if ("OrderPaid".equals(event.getType())) {
                    routingKey = ORDER_PAID_ROUTING_KEY;
                } else {
                    log.warn("Unknown event type: {}, skipping...", event.getType());
                    continue;
                }

                // 3. Gửi event lên RabbitMQ
                Object payloadToSend = event.getPayload();

                // Để tránh JSON bị "double encode" (\"{...}\"),
                // với OrderCreated ta parse JSON thành object rồi mới gửi.
                if ("OrderCreated".equals(event.getType())) {
                    try {
                        payloadToSend = objectMapper.readValue(
                                event.getPayload(),
                                OrderCreatedEventPayload.class);
                    } catch (Exception ex) {
                        log.error("Failed to deserialize OrderCreated payload for event id={}", event.getId(), ex);
                        // Nếu lỗi, fallback gửi raw JSON string như cũ
                        payloadToSend = event.getPayload();
                    }
                }

                rabbitTemplate.convertAndSend(
                        ORDER_EXCHANGE,
                        routingKey,
                        payloadToSend);

                // 4. Gửi thành công, cập nhật trạng thái
                event.setStatus(EventStatus.PROCESSED);
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
