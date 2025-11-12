package com.example.payment.application.usecase;

import com.example.payment.application.dto.PaymentStatisticsResponse;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetMerchantPaymentStatisticsUseCase {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public PaymentStatisticsResponse execute(Long merchantId, LocalDateTime fromDate, LocalDateTime toDate) {
        log.info("Getting payment statistics for merchant: {} from {} to {}", merchantId, fromDate, toDate);

        // ===== BƯỚC 1: VALIDATE VÀ SET DEFAULT DATE RANGE =====
        if (fromDate == null) {
            fromDate = LocalDateTime.now().minusDays(30);
            log.debug("fromDate not provided, using default: last 30 days");
        }
        if (toDate == null) {
            toDate = LocalDateTime.now();
            log.debug("toDate not provided, using default: now");
        }
        
        // Validate date range
        if (fromDate.isAfter(toDate)) {
            log.warn("Invalid date range: fromDate {} is after toDate {}", fromDate, toDate);
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }
        
        log.info("Calculating statistics for merchant {} in date range: {} to {}", merchantId, fromDate, toDate);

        // ===== BƯỚC 2: ĐẾM SỐ LƯỢNG PAYMENTS THEO TỪNG STATUS =====
        log.debug("Step 1: Counting payments by status for merchant {}", merchantId);
        
        Long successfulPayments = paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.SUCCESS, fromDate, toDate);
        if (successfulPayments == null) {
            successfulPayments = 0L;
        }
        log.debug("Successful payments count: {}", successfulPayments);

        Long failedPayments = paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.FAILED, fromDate, toDate);
        if (failedPayments == null) {
            failedPayments = 0L;
        }
        log.debug("Failed payments count: {}", failedPayments);

        Long pendingPayments = paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.PENDING, fromDate, toDate);
        if (pendingPayments == null) {
            pendingPayments = 0L;
        }
        log.debug("Pending payments count: {}", pendingPayments);

        Long refundedPayments = paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.REFUNDED, fromDate, toDate);
        if (refundedPayments == null) {
            refundedPayments = 0L;
        }
        log.debug("Refunded payments count: {}", refundedPayments);

        Long totalOrders = successfulPayments + failedPayments + pendingPayments + refundedPayments;
        log.debug("Total orders count: {}", totalOrders);

        // ===== BƯỚC 3: TÍNH TỔNG DOANH THU =====
        log.debug("Step 2: Calculating total revenue for merchant {}", merchantId);
        
        // Bước 3.1: Tính tổng doanh thu từ các payment SUCCESS
        BigDecimal totalSuccessRevenue = paymentRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.SUCCESS, fromDate, toDate);
        if (totalSuccessRevenue == null) {
            totalSuccessRevenue = BigDecimal.ZERO;
        }
        log.debug("Total revenue from SUCCESS payments: {}", totalSuccessRevenue);

        // Bước 3.2: Tính tổng số tiền đã hoàn lại từ các payment REFUNDED
        BigDecimal totalRefundedAmount = paymentRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.REFUNDED, fromDate, toDate);
        if (totalRefundedAmount == null) {
            totalRefundedAmount = BigDecimal.ZERO;
        }
        log.debug("Total refunded amount: {}", totalRefundedAmount);

        // Bước 3.3: Tính tổng doanh thu thực tế = Tổng SUCCESS - Tổng REFUNDED
        BigDecimal totalRevenue = totalSuccessRevenue.subtract(totalRefundedAmount);
        
        // Đảm bảo totalRevenue không âm
        if (totalRevenue.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Total revenue is negative ({}), setting to 0. This may indicate data inconsistency.", totalRevenue);
            totalRevenue = BigDecimal.ZERO;
        }
        
        log.info("Final total revenue for merchant {}: {} (SUCCESS: {}, REFUNDED: {})", 
                merchantId, totalRevenue, totalSuccessRevenue, totalRefundedAmount);

        // ===== BƯỚC 4: BUILD RESPONSE =====
        PaymentStatisticsResponse response = PaymentStatisticsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .successfulPayments(successfulPayments)
                .failedPayments(failedPayments)
                .pendingPayments(pendingPayments)
                .refundedPayments(refundedPayments)
                .build();
        
        log.info("Statistics calculated successfully for merchant {}: totalRevenue={}, totalOrders={}", 
                merchantId, totalRevenue, totalOrders);
        
        return response;
    }
}

