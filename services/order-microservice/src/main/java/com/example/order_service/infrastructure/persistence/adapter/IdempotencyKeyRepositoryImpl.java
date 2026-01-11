package com.example.order_service.infrastructure.persistence.adapter;

import com.example.order_service.domain.entities.IdempotencyKey;
import com.example.order_service.domain.repository.IdempotencyKeyRepository;
import com.example.order_service.infrastructure.persistence.mapper.IdempotencyKeyMapper;
import com.example.order_service.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adapter implementing domain IdempotencyKeyRepository using JPA
 * infrastructure.
 * Bridges the gap between domain layer and infrastructure layer.
 */
@Repository
@RequiredArgsConstructor
public class IdempotencyKeyRepositoryImpl implements IdempotencyKeyRepository {

    private final IdempotencyKeyJpaRepository idempotencyKeyJpaRepository;

    @Override
    public IdempotencyKey save(IdempotencyKey idempotencyKey) {
        var jpaEntity = IdempotencyKeyMapper.toJpaEntity(idempotencyKey);
        var savedEntity = idempotencyKeyJpaRepository.save(jpaEntity);
        return IdempotencyKeyMapper.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<IdempotencyKey> findById(Long id) {
        return idempotencyKeyJpaRepository.findById(id)
                .map(IdempotencyKeyMapper::toDomainEntity);
    }

    @Override
    public void deleteById(Long id) {
        idempotencyKeyJpaRepository.deleteById(id);
    }

    @Override
    public Optional<IdempotencyKey> findByUserIdAndIdemKey(Long userId, String idemKey) {
        return idempotencyKeyJpaRepository.findByUserIdAndIdemKey(userId, idemKey)
                .map(IdempotencyKeyMapper::toDomainEntity);
    }

    @Override
    public boolean existsByUserIdAndIdemKey(Long userId, String idemKey) {
        return idempotencyKeyJpaRepository.existsByUserIdAndIdemKey(userId, idemKey);
    }
}
