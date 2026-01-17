package com.example.productservice.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Pure domain entity representing a StockDeductionRecord.
 * Contains business logic and domain rules, independent of persistence framework.
 * No JPA annotations - this is a pure business object.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockDeductionRecord {
    
    private Long id;
    private Long orderId;
    private LocalDateTime createdAt;

    public StockDeductionRecord(Long orderId) {
        this.orderId = orderId;
        this.createdAt = LocalDateTime.now();
    }
}
