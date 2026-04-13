package com.example.droneservice.infrastructure.persistence.adapter;

import com.example.droneservice.domain.entities.OutboxEvent;
import com.example.droneservice.domain.repository.OutboxEventRepository;
import com.example.droneservice.domain.valueobjects.EventStatus;
import com.example.droneservice.infrastructure.persistence.mapper.OutboxEventMapper;
import com.example.droneservice.infrastructure.persistence.repository.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing domain OutboxEventRepository using JPA infrastructure.
 */
@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        var jpaEntity = OutboxEventMapper.toJpaEntity(outboxEvent);
        var savedEntity = outboxEventJpaRepository.save(jpaEntity);
        return OutboxEventMapper.toDomainEntity(savedEntity);
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
    public void deleteById(Long id) {
        outboxEventJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return outboxEventJpaRepository.existsById(id);
    }

    @Override
    public List<OutboxEvent> findByStatus(EventStatus status) {
        return outboxEventJpaRepository.findByStatus(status).stream()
                .map(OutboxEventMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<OutboxEvent> findFailedEventsBefore(EventStatus status, LocalDateTime cutoffTime) {
        return outboxEventJpaRepository.findFailedEventsBefore(status, cutoffTime).stream()
                .map(OutboxEventMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByStatusAndCreatedAtBefore(EventStatus status, LocalDateTime cutoffTime) {
        outboxEventJpaRepository.deleteByStatusAndCreatedAtBefore(status, cutoffTime);
    }
}
