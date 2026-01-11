package com.example.order_service.infrastructure.persistence.mapper;

import com.example.order_service.domain.entities.IdempotencyKey;
import com.example.order_service.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;

/**
 * Mapper for converting between IdempotencyKey domain entity and JPA entity.
 */
public class IdempotencyKeyMapper {

    /**
     * Convert JPA entity to domain entity
     */
    public static IdempotencyKey toDomainEntity(IdempotencyKeyJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        return IdempotencyKey.builder()
                .id(jpaEntity.getId())
                .userId(jpaEntity.getUserId())
                .idemKey(jpaEntity.getIdemKey())
                .requestHash(jpaEntity.getRequestHash())
                .orderId(jpaEntity.getOrderId())
                .createdAt(jpaEntity.getCreatedAt())
                .build();
    }

    /**
     * Convert domain entity to JPA entity
     */
    public static IdempotencyKeyJpaEntity toJpaEntity(IdempotencyKey domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        return IdempotencyKeyJpaEntity.builder()
                .id(domainEntity.getId())
                .userId(domainEntity.getUserId())
                .idemKey(domainEntity.getIdemKey())
                .requestHash(domainEntity.getRequestHash())
                .orderId(domainEntity.getOrderId())
                .createdAt(domainEntity.getCreatedAt())
                .build();
    }
}
