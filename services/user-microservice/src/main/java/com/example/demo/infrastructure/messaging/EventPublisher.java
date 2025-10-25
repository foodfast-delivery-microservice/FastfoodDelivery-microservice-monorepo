package com.example.demo.infrastructure.messaging;

import com.example.demo.interfaces.rest.dto.event.UserUpdatedEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
    private final RabbitTemplate rabbitTemplate;

    // định nghĩa tên của exchange
    private static final String USER_EVENTS_EXCHANGE = "user.events";

    public void publishUserUpdated(UserUpdatedEventDTO userUpdatedEventDTO) {
        try{
            log.info("Publishing UserUpdateEvent for userId: {}", userUpdatedEventDTO.getUserId());

            // Gửi event đến exchange với routing key là "user.updated"
            rabbitTemplate.convertAndSend(USER_EVENTS_EXCHANGE, "user.updated", userUpdatedEventDTO);
        } catch (Exception e) {
            log.error("Failed to publish UserUpdatedEvent for userId: {}", userUpdatedEventDTO.getUserId(), e);
        }
    }
}
