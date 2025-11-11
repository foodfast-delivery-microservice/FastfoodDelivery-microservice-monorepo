package com.example.payment.domain.repository;

import com.example.payment.domain.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    boolean existsByOrderId(Long orderId);
}
