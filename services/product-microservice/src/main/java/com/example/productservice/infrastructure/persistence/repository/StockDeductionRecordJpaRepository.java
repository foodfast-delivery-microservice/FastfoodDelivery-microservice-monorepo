package com.example.productservice.infrastructure.persistence.repository;

import com.example.productservice.infrastructure.persistence.entity.StockDeductionRecordJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for StockDeductionRecordJpaEntity persistence.
 * This is the infrastructure layer repository with Spring Data JPA.
 */
@Repository
public interface StockDeductionRecordJpaRepository extends JpaRepository<StockDeductionRecordJpaEntity, Long> {
    boolean existsByOrderId(Long orderId);
}
