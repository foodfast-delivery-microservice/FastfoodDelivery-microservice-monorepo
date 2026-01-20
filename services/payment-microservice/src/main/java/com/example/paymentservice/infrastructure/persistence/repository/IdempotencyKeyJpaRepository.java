package com.example.paymentservice.infrastructure.persistence.repository;

import com.example.paymentservice.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyJpaEntity, Long> {
    
    boolean existsByOrderId(Long orderId);
}
