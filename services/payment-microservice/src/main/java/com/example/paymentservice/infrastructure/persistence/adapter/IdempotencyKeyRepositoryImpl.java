package com.example.paymentservice.infrastructure.persistence.adapter;

import com.example.paymentservice.domain.entities.IdempotencyKey;
import com.example.paymentservice.domain.repository.IdempotencyKeyRepository;
import com.example.paymentservice.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import com.example.paymentservice.infrastructure.persistence.mapper.IdempotencyKeyMapper;
import com.example.paymentservice.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adapter implementing domain IdempotencyKeyRepository using JPA infrastructure.
 * Bridges the gap between domain layer and infrastructure layer.
 */
@Repository
@RequiredArgsConstructor
public class IdempotencyKeyRepositoryImpl implements IdempotencyKeyRepository {

    private final IdempotencyKeyJpaRepository idempotencyKeyJpaRepository;

    @Override
    public IdempotencyKey save(IdempotencyKey idempotencyKey) {
        IdempotencyKeyJpaEntity jpaEntity = IdempotencyKeyMapper.toJpaEntity(idempotencyKey);
        IdempotencyKeyJpaEntity saved = idempotencyKeyJpaRepository.save(jpaEntity);
        return IdempotencyKeyMapper.toDomainEntity(saved);
    }

    @Override
    public Optional<IdempotencyKey> findById(Long id) {
        return idempotencyKeyJpaRepository.findById(id)
                .map(IdempotencyKeyMapper::toDomainEntity);
    }

    @Override
    public boolean existsByOrderId(Long orderId) {
        return idempotencyKeyJpaRepository.existsByOrderId(orderId);
    }
}
