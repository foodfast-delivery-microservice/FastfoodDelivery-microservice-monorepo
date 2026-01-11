package com.example.order_service.infrastructure.persistence.mapper;

import com.example.order_service.domain.entities.OutboxEvent;
import com.example.order_service.infrastructure.persistence.entity.OutboxEventJpaEntity;

/**
 * Mapper to convert between OutboxEvent domain entity and OutboxEventJpaEntity.
 */
public class OutboxEventMapper {

    /**
     * Convert domain entity to JPA entity
     */
    public static OutboxEventJpaEntity toJpaEntity(OutboxEvent domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        OutboxEventJpaEntity jpaEntity = new OutboxEventJpaEntity();
        jpaEntity.setId(domainEntity.getId());
        jpaEntity.setAggregateType(domainEntity.getAggregateType());
        jpaEntity.setAggregateId(domainEntity.getAggregateId());
        jpaEntity.setType(domainEntity.getType());
        jpaEntity.setPayload(domainEntity.getPayload());
        jpaEntity.setStatus(domainEntity.getStatus());
        jpaEntity.setCreatedAt(domainEntity.getCreatedAt());

        return jpaEntity;
    }

    /**
     * Convert JPA entity to domain entity
     */
    public static OutboxEvent toDomainEntity(OutboxEventJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        OutboxEvent domainEntity = OutboxEvent.builder()
                .id(jpaEntity.getId())
                .aggregateType(jpaEntity.getAggregateType())
                .aggregateId(jpaEntity.getAggregateId())
                .type(jpaEntity.getType())
                .payload(jpaEntity.getPayload())
                .status(jpaEntity.getStatus())
                .createdAt(jpaEntity.getCreatedAt())
                .build();

        return domainEntity;
    }
}
