package com.example.notificationservice.infrastructure.persistence.repository;

import com.example.notificationservice.infrastructure.persistence.entity.WebhookEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEventJpaRepository extends JpaRepository<WebhookEventJpaEntity, Long> {
    Optional<WebhookEventJpaEntity> findBySgEventId(String sgEventId);
    boolean existsBySgEventId(String sgEventId);
}
