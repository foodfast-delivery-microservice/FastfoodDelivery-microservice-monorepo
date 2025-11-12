package com.example.payment.domain.repository;

import com.example.payment.domain.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByOrderId(Long orderId);
    List<Payment> findByUserId(Long userId);
    
    // Merchant queries
    Page<Payment> findByMerchantId(Long merchantId, Pageable pageable);
    
    Page<Payment> findByMerchantIdAndStatus(Long merchantId, Payment.Status status, Pageable pageable);
    
    Page<Payment> findByMerchantIdAndCreatedAtBetween(
            Long merchantId, 
            LocalDateTime fromDate, 
            LocalDateTime toDate, 
            Pageable pageable
    );
    
    Page<Payment> findByMerchantIdAndStatusAndCreatedAtBetween(
            Long merchantId, 
            Payment.Status status,
            LocalDateTime fromDate, 
            LocalDateTime toDate, 
            Pageable pageable
    );
    
    // Statistics queries
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.merchantId = :merchantId AND p.status = :status AND p.createdAt BETWEEN :fromDate AND :toDate")
    BigDecimal sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
            @Param("merchantId") Long merchantId,
            @Param("status") Payment.Status status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.merchantId = :merchantId AND p.status = :status AND p.createdAt BETWEEN :fromDate AND :toDate")
    Long countByMerchantIdAndStatusAndCreatedAtBetween(
            @Param("merchantId") Long merchantId,
            @Param("status") Payment.Status status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}



