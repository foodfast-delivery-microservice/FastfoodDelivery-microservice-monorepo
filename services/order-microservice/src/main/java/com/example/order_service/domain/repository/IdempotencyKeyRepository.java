package com.example.order_service.domain.repository;

import com.example.order_service.domain.entities.IdempotencyKey;

import java.util.Optional;

/**
 * Pure domain repository interface for IdempotencyKey.
 * No framework dependencies - follows clean architecture principles.
 * Used for preventing duplicate order creation from same request.
 */
public interface IdempotencyKeyRepository {

    // Basic CRUD
    IdempotencyKey save(IdempotencyKey idempotencyKey);

    Optional<IdempotencyKey> findById(Long id);

    void deleteById(Long id);

    // Business queries
    Optional<IdempotencyKey> findByUserIdAndIdemKey(Long userId, String idemKey);

    boolean existsByUserIdAndIdemKey(Long userId, String idemKey);
}
