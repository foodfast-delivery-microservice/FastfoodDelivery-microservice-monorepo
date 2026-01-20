package com.example.paymentservice.infrastructure.persistence.adapter;

import com.example.paymentservice.domain.entities.OutboxEvent;
import com.example.paymentservice.domain.repository.OutboxEventRepository;
import com.example.paymentservice.domain.valueobjects.EventStatus;
import com.example.paymentservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.example.paymentservice.infrastructure.persistence.mapper.OutboxEventMapper;
import com.example.paymentservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing domain OutboxEventRepository using JPA infrastructure.
 * Bridges the gap between domain layer and infrastructure layer.
 */
@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        OutboxEventJpaEntity jpaEntity = OutboxEventMapper.toJpaEntity(outboxEvent);
        OutboxEventJpaEntity saved = outboxEventJpaRepository.save(jpaEntity);
        return OutboxEventMapper.toDomainEntity(saved);
    }

    @Override
    public Optional<OutboxEvent> findById(Long id) {
        return outboxEventJpaRepository.findById(id)
                .map(OutboxEventMapper::toDomainEntity);
    }

    @Override
    public List<OutboxEvent> findAll() {
        return outboxEventJpaRepository.findAll().stream()
                .map(OutboxEventMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<OutboxEvent> findByStatus(EventStatus status) {
        return outboxEventJpaRepository.findByStatus(status).stream()
                .map(OutboxEventMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}
