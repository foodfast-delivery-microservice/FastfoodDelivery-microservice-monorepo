package com.example.productservice.infrastructure.persistence.adapter;

import com.example.productservice.domain.entities.StockDeductionRecord;
import com.example.productservice.domain.repository.StockDeductionRecordRepository;
import com.example.productservice.infrastructure.persistence.entity.StockDeductionRecordJpaEntity;
import com.example.productservice.infrastructure.persistence.mapper.StockDeductionRecordMapper;
import com.example.productservice.infrastructure.persistence.repository.StockDeductionRecordJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Adapter implementing domain StockDeductionRecordRepository using JPA infrastructure.
 * Bridges the gap between domain layer and infrastructure layer.
 */
@Repository
@RequiredArgsConstructor
public class StockDeductionRecordRepositoryImpl implements StockDeductionRecordRepository {

    private final StockDeductionRecordJpaRepository stockDeductionRecordJpaRepository;

    @Override
    public StockDeductionRecord save(StockDeductionRecord record) {
        StockDeductionRecordJpaEntity jpaEntity = StockDeductionRecordMapper.toJpaEntity(record);
        StockDeductionRecordJpaEntity saved = stockDeductionRecordJpaRepository.save(jpaEntity);
        return StockDeductionRecordMapper.toDomainEntity(saved);
    }

    @Override
    public boolean existsByOrderId(Long orderId) {
        return stockDeductionRecordJpaRepository.existsByOrderId(orderId);
    }
}
