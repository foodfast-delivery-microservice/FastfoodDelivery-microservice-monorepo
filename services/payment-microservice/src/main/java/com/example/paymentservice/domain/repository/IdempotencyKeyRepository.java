package com.example.paymentservice.domain.repository;

import com.example.paymentservice.domain.entities.IdempotencyKey;

import java.util.Optional;

/**
 * Pure domain repository interface for IdempotencyKey aggregate.
 */
public interface IdempotencyKeyRepository {
    
    // Basic CRUD
    IdempotencyKey save(IdempotencyKey idempotencyKey);
    
    Optional<IdempotencyKey> findById(Long id);
    
    // Business queries
    boolean existsByOrderId(Long orderId);
}
