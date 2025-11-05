package com.example.order_service.infrastructure.messaging;



import com.example.order_service.domain.model.EventStatus;
import com.example.order_service.domain.model.OutboxEvent;
import com.example.order_service.domain.repository.OutboxEventRepository;
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

    // Tên Exchange (sàn giao dịch)
    public static final String ORDER_EXCHANGE = "order_exchange";
    // Tên routing key (định tuyến)
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

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
                // 2. Gửi event lên RabbitMQ
                rabbitTemplate.convertAndSend(
                        ORDER_EXCHANGE,
                        ORDER_CREATED_ROUTING_KEY,
                        event.getPayload() // Gửi nội dung JSON đi
                );

                // 3. Gửi thành công, cập nhật trạng thái
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
