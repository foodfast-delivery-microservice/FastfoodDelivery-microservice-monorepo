package com.example.order_service.infrastructure.persistence.repository;

import com.example.order_service.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for IdempotencyKey persistence.
 * Works with JPA entities only.
 */
@Repository
public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyJpaEntity, Long> {

    Optional<IdempotencyKeyJpaEntity> findByUserIdAndIdemKey(Long userId, String idemKey);

    boolean existsByUserIdAndIdemKey(Long userId, String idemKey);
}
