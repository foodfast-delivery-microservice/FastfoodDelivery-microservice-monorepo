package com.example.paymentservice.infrastructure.persistence.mapper;

import com.example.paymentservice.domain.entities.IdempotencyKey;
import com.example.paymentservice.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;

/**
 * Mapper to convert between IdempotencyKey domain entity and IdempotencyKeyJpaEntity.
 */
public class IdempotencyKeyMapper {

    /**
     * Convert domain entity to JPA entity
     */
    public static IdempotencyKeyJpaEntity toJpaEntity(IdempotencyKey domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        IdempotencyKeyJpaEntity jpaEntity = new IdempotencyKeyJpaEntity();
        jpaEntity.setId(domainEntity.getId());
        jpaEntity.setOrderId(domainEntity.getOrderId());
        jpaEntity.setPaymentId(domainEntity.getPaymentId());
        jpaEntity.setCreatedAt(domainEntity.getCreatedAt());

        return jpaEntity;
    }

    /**
     * Convert JPA entity to domain entity
     */
    public static IdempotencyKey toDomainEntity(IdempotencyKeyJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        IdempotencyKey domainEntity = new IdempotencyKey();
        domainEntity.setId(jpaEntity.getId());
        domainEntity.setOrderId(jpaEntity.getOrderId());
        domainEntity.setPaymentId(jpaEntity.getPaymentId());
        domainEntity.setCreatedAt(jpaEntity.getCreatedAt());

        return domainEntity;
    }
}
