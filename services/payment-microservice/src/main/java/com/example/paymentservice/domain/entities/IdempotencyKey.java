package com.example.paymentservice.domain.entities;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Pure domain entity representing an IdempotencyKey.
 * Contains business logic and domain rules, independent of persistence framework.
 * No JPA annotations - this is a pure business object.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    private Long id;
    private Long orderId;
    private Long paymentId;
    private LocalDateTime createdAt;

    public IdempotencyKey(Long orderId, Long paymentId) {
        this.orderId = orderId;
        this.paymentId = paymentId;
    }
}
