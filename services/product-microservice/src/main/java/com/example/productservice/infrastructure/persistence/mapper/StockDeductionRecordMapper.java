package com.example.productservice.infrastructure.persistence.mapper;

import com.example.productservice.domain.entities.StockDeductionRecord;
import com.example.productservice.infrastructure.persistence.entity.StockDeductionRecordJpaEntity;

/**
 * Mapper to convert between StockDeductionRecord domain entity and StockDeductionRecordJpaEntity.
 */
public class StockDeductionRecordMapper {

    /**
     * Convert domain entity to JPA entity
     */
    public static StockDeductionRecordJpaEntity toJpaEntity(StockDeductionRecord domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        StockDeductionRecordJpaEntity jpaEntity = new StockDeductionRecordJpaEntity();
        jpaEntity.setId(domainEntity.getId());
        jpaEntity.setOrderId(domainEntity.getOrderId());
        jpaEntity.setCreatedAt(domainEntity.getCreatedAt());

        return jpaEntity;
    }

    /**
     * Convert JPA entity to domain entity
     */
    public static StockDeductionRecord toDomainEntity(StockDeductionRecordJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        StockDeductionRecord domainEntity = new StockDeductionRecord();
        domainEntity.setId(jpaEntity.getId());
        domainEntity.setOrderId(jpaEntity.getOrderId());
        domainEntity.setCreatedAt(jpaEntity.getCreatedAt());

        return domainEntity;
    }
}
