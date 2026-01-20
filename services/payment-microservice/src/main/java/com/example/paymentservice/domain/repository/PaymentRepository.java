package com.example.paymentservice.domain.repository;

import com.example.paymentservice.domain.entities.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Pure domain repository interface for Payment aggregate.
 * Note: Contains some Spring Data-specific methods (Page, Pageable) for pragmatic reasons.
 */
public interface PaymentRepository {
    
    // Basic CRUD
    Payment save(Payment payment);
    
    Optional<Payment> findById(Long id);
    
    Optional<Payment> findByOrderId(Long orderId);
    
    List<Payment> findByUserId(Long userId);
    
    // Merchant queries
    Page<Payment> findByMerchantId(Long merchantId, Pageable pageable);
    
    Page<Payment> findByMerchantIdAndStatus(Long merchantId, Payment.Status status, Pageable pageable);
    
    List<Payment> findByMerchantIdAndStatus(Long merchantId, Payment.Status status);
    
    Page<Payment> findByMerchantIdAndCreatedAtBetween(
                    Long merchantId,
                    LocalDateTime fromDate,
                    LocalDateTime toDate,
                    Pageable pageable);
    
    Page<Payment> findByMerchantIdAndStatusAndCreatedAtBetween(
                    Long merchantId,
                    Payment.Status status,
                    LocalDateTime fromDate,
                    LocalDateTime toDate,
                    Pageable pageable);
    
    // Statistics queries
    BigDecimal sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                    Long merchantId,
                    Payment.Status status,
                    LocalDateTime fromDate,
                    LocalDateTime toDate);
    
    Long countByMerchantIdAndStatusAndCreatedAtBetween(
                    Long merchantId,
                    Payment.Status status,
                    LocalDateTime fromDate,
                    LocalDateTime toDate);
    
    // Lifetime statistics queries
    BigDecimal sumAmountByMerchantIdAndStatus(
                    Long merchantId,
                    Payment.Status status);
    
    Long countByMerchantIdAndStatus(
                    Long merchantId,
                    Payment.Status status);
}
