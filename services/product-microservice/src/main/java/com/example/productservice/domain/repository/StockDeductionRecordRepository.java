package com.example.productservice.domain.repository;

import com.example.productservice.domain.entities.StockDeductionRecord;

/**
 * Domain repository interface for StockDeductionRecord.
 * This is a pure domain interface with no framework dependencies.
 * Implementations are in the infrastructure layer.
 */
public interface StockDeductionRecordRepository {
    
    StockDeductionRecord save(StockDeductionRecord record);
    
    boolean existsByOrderId(Long orderId);
}
