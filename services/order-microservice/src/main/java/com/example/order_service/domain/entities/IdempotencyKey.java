package com.example.order_service.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Pure domain entity for Idempotency Key.
 * Ensures request deduplication by tracking idempotency keys.
 * No framework dependencies - follows clean architecture principles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    private Long id;
    private Long userId;
    private String idemKey;
    private String requestHash;
    private Long orderId;
    private LocalDateTime createdAt;

    /**
     * Factory method to create new IdempotencyKey
     */
    public static IdempotencyKey create(Long userId, String idemKey, String requestHash, Long orderId) {
        return IdempotencyKey.builder()
                .userId(userId)
                .idemKey(idemKey)
                .requestHash(requestHash)
                .orderId(orderId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Business validation
     */
    public boolean isValid() {
        return userId != null && idemKey != null && requestHash != null && orderId != null;
    }
}
