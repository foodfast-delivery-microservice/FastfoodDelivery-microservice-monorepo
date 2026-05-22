package com.example.notificationservice.infrastructure.persistence.adapter;

import com.example.notificationservice.domain.port.WebhookEventPort;
import com.example.notificationservice.infrastructure.persistence.entity.WebhookEventJpaEntity;
import com.example.notificationservice.infrastructure.persistence.repository.WebhookEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventAdapter implements WebhookEventPort {

    private final WebhookEventJpaRepository webhookEventJpaRepository;

    @Override
    public boolean isEventProcessed(String sgEventId) {
        return webhookEventJpaRepository.existsBySgEventId(sgEventId);
    }

    @Override
    @Transactional
    public void recordEventProcessed(String sgEventId, String eventType, String email, Long userId, Long notificationId, String eventData) {
        if (webhookEventJpaRepository.existsBySgEventId(sgEventId)) {
            log.debug("Event {} already recorded, skipping duplicate", sgEventId);
            return;
        }

        WebhookEventJpaEntity entity = WebhookEventJpaEntity.builder()
                .sgEventId(sgEventId)
                .eventType(eventType)
                .email(email)
                .userId(userId)
                .notificationId(notificationId)
                .eventData(eventData)
                .build();

        webhookEventJpaRepository.save(entity);
        log.info("Recorded webhook event: sgEventId={}, type={}, email={}", sgEventId, eventType, email);
    }
}
