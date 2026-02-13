package com.example.paymentservice.domain.repository;

import com.example.paymentservice.domain.entities.Payment;
import com.example.paymentservice.domain.valueobjects.PageRequest;
import com.example.paymentservice.domain.valueobjects.PageResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Pure domain repository interface for Payment aggregate.
 * All methods use domain value objects, no framework dependencies.
 */
public interface PaymentRepository {
    
    // Basic CRUD
    Payment save(Payment payment);
    
    Optional<Payment> findById(Long id);
    
    Optional<Payment> findByOrderId(Long orderId);
    
    List<Payment> findByUserId(Long userId);
    
    // Merchant queries with pagination
    PageResult<Payment> findByMerchantId(Long merchantId, PageRequest pageRequest);
    
    PageResult<Payment> findByMerchantIdAndStatus(Long merchantId, Payment.Status status, PageRequest pageRequest);
    
    List<Payment> findByMerchantIdAndStatus(Long merchantId, Payment.Status status);
    
    PageResult<Payment> findByMerchantIdAndCreatedAtBetween(
                    Long merchantId,
                    LocalDateTime fromDate,
                    LocalDateTime toDate,
                    PageRequest pageRequest);
    
    PageResult<Payment> findByMerchantIdAndStatusAndCreatedAtBetween(
                    Long merchantId,
                    Payment.Status status,
                    LocalDateTime fromDate,
                    LocalDateTime toDate,
                    PageRequest pageRequest);
    
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
