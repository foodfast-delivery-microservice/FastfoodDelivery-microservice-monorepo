package com.example.paymentservice.infrastructure.persistence.repository;

import com.example.paymentservice.domain.entities.Payment;
import com.example.paymentservice.infrastructure.persistence.entity.PaymentJpaEntity;
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
public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long>, JpaSpecificationExecutor<PaymentJpaEntity> {
    
    Optional<PaymentJpaEntity> findByOrderId(Long orderId);
    
    List<PaymentJpaEntity> findByUserId(Long userId);
    
    // Merchant queries
    Page<PaymentJpaEntity> findByMerchantId(Long merchantId, Pageable pageable);
    
    Page<PaymentJpaEntity> findByMerchantIdAndStatus(Long merchantId, Payment.Status status, Pageable pageable);
    
    List<PaymentJpaEntity> findByMerchantIdAndStatus(Long merchantId, Payment.Status status);
    
    Page<PaymentJpaEntity> findByMerchantIdAndCreatedAtBetween(
                    Long merchantId,
                    LocalDateTime fromDate,
                    LocalDateTime toDate,
                    Pageable pageable);
    
    Page<PaymentJpaEntity> findByMerchantIdAndStatusAndCreatedAtBetween(
                    Long merchantId,
                    Payment.Status status,
                    LocalDateTime fromDate,
                    LocalDateTime toDate,
                    Pageable pageable);
    
    // Statistics queries
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentJpaEntity p WHERE p.merchantId = :merchantId AND p.status = :status AND p.createdAt BETWEEN :fromDate AND :toDate")
    BigDecimal sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                    @Param("merchantId") Long merchantId,
                    @Param("status") Payment.Status status,
                    @Param("fromDate") LocalDateTime fromDate,
                    @Param("toDate") LocalDateTime toDate);
    
    @Query("SELECT COUNT(p) FROM PaymentJpaEntity p WHERE p.merchantId = :merchantId AND p.status = :status AND p.createdAt BETWEEN :fromDate AND :toDate")
    Long countByMerchantIdAndStatusAndCreatedAtBetween(
                    @Param("merchantId") Long merchantId,
                    @Param("status") Payment.Status status,
                    @Param("fromDate") LocalDateTime fromDate,
                    @Param("toDate") LocalDateTime toDate);
    
    // Lifetime statistics queries
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentJpaEntity p WHERE p.merchantId = :merchantId AND p.status = :status")
    BigDecimal sumAmountByMerchantIdAndStatus(
                    @Param("merchantId") Long merchantId,
                    @Param("status") Payment.Status status);
    
    @Query("SELECT COUNT(p) FROM PaymentJpaEntity p WHERE p.merchantId = :merchantId AND p.status = :status")
    Long countByMerchantIdAndStatus(
                    @Param("merchantId") Long merchantId,
                    @Param("status") Payment.Status status);
}
