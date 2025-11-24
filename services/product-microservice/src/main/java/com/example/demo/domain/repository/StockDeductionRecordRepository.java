package com.example.demo.domain.repository;

import com.example.demo.domain.model.StockDeductionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDeductionRecordRepository extends JpaRepository<StockDeductionRecord, Long> {
    boolean existsByOrderId(Long orderId);
}


