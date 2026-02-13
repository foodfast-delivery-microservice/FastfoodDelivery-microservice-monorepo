package com.example.droneservice.domain.repository;

import com.example.droneservice.domain.entities.OutboxEvent;
import com.example.droneservice.domain.valueobjects.EventStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Pure domain repository interface for OutboxEvent aggregate.
 * No framework dependencies - follows clean architecture principles.
 */
public interface OutboxEventRepository {

    // Basic CRUD
    OutboxEvent save(OutboxEvent outboxEvent);

    Optional<OutboxEvent> findById(Long id);

    List<OutboxEvent> findAll();

    void deleteById(Long id);

    boolean existsById(Long id);

    // Business queries
    List<OutboxEvent> findByStatus(EventStatus status);

    List<OutboxEvent> findFailedEventsBefore(EventStatus status, LocalDateTime cutoffTime);

    void deleteByStatusAndCreatedAtBefore(EventStatus status, LocalDateTime cutoffTime);
}
