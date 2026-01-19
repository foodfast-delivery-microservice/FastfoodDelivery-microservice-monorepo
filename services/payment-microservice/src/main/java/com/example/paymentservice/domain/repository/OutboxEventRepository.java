package com.example.paymentservice.domain.repository;

import com.example.paymentservice.domain.entities.OutboxEvent;
import com.example.paymentservice.domain.valueobjects.EventStatus;

import java.util.List;
import java.util.Optional;

/**
 * Pure domain repository interface for OutboxEvent aggregate.
 */
public interface OutboxEventRepository {
    
    // Basic CRUD
    OutboxEvent save(OutboxEvent outboxEvent);
    
    Optional<OutboxEvent> findById(Long id);
    
    /**
     * Retrieve all outbox events regardless of status.
     * Useful for idempotency checks when replaying events.
     */
    List<OutboxEvent> findAll();
    
    // Business queries
    List<OutboxEvent> findByStatus(EventStatus status);
}
