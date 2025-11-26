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

        // ===== BƯỚC 1: VALIDATE INPUT =====
        if (merchantId == null || merchantId <= 0) {
            log.warn("Invalid merchantId: {}", merchantId);
            throw new IllegalArgumentException("merchantId must be a positive number");
        }

        // ===== BƯỚC 2: VALIDATE VÀ SET DEFAULT DATE RANGE =====
        boolean isLifetime = (fromDate == null && toDate == null);

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

        // Validate date range không quá lớn (ví dụ: tối đa 1 năm)
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate);
        if (!isLifetime && daysBetween > 365) {
            log.warn("Date range too large: {} days. Maximum allowed is 365 days.", daysBetween);
            throw new IllegalArgumentException("Date range cannot exceed 365 days");
        }

        log.info("Calculating statistics for merchant {} in date range: {} to {} (Lifetime: {})", merchantId, fromDate,
                toDate, isLifetime);

        // ===== BƯỚC 3: ĐẾM SỐ LƯỢNG PAYMENTS THEO TỪNG STATUS =====
        log.debug("Step 3: Counting payments by status for merchant {}", merchantId);

        Long successfulPayments;
        Long failedPayments;
        Long pendingPayments;
        Long refundedPayments;

        if (isLifetime) {
            successfulPayments = paymentRepository.countByMerchantIdAndStatus(merchantId, Payment.Status.SUCCESS);
            failedPayments = paymentRepository.countByMerchantIdAndStatus(merchantId, Payment.Status.FAILED);
            pendingPayments = paymentRepository.countByMerchantIdAndStatus(merchantId, Payment.Status.PENDING);
            refundedPayments = paymentRepository.countByMerchantIdAndStatus(merchantId, Payment.Status.REFUNDED);
        } else {
            successfulPayments = paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                    merchantId, Payment.Status.SUCCESS, fromDate, toDate);
            failedPayments = paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                    merchantId, Payment.Status.FAILED, fromDate, toDate);
            pendingPayments = paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                    merchantId, Payment.Status.PENDING, fromDate, toDate);
            refundedPayments = paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                    merchantId, Payment.Status.REFUNDED, fromDate, toDate);
        }

        if (successfulPayments == null)
            successfulPayments = 0L;
        if (failedPayments == null)
            failedPayments = 0L;
        if (pendingPayments == null)
            pendingPayments = 0L;
        if (refundedPayments == null)
            refundedPayments = 0L;

        log.debug("Successful payments count: {}", successfulPayments);
        log.debug("Failed payments count: {}", failedPayments);
        log.debug("Pending payments count: {}", pendingPayments);
        log.debug("Refunded payments count: {}", refundedPayments);

        Long totalOrders = successfulPayments + failedPayments + pendingPayments + refundedPayments;
        log.debug("Total orders count: {}", totalOrders);

        // ===== BƯỚC 4: TÍNH TỔNG DOANH THU =====
        log.debug("Step 4: Calculating total revenue for merchant {}", merchantId);

        BigDecimal totalSuccessRevenue;
        BigDecimal totalRefundedAmount;

        if (isLifetime) {
            totalSuccessRevenue = paymentRepository.sumAmountByMerchantIdAndStatus(merchantId, Payment.Status.SUCCESS);
            totalRefundedAmount = paymentRepository.sumAmountByMerchantIdAndStatus(merchantId, Payment.Status.REFUNDED);
        } else {
            totalSuccessRevenue = paymentRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                    merchantId, Payment.Status.SUCCESS, fromDate, toDate);
            totalRefundedAmount = paymentRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                    merchantId, Payment.Status.REFUNDED, fromDate, toDate);
        }

        if (totalSuccessRevenue == null) {
            totalSuccessRevenue = BigDecimal.ZERO;
        }
        log.debug("Total revenue from SUCCESS payments: {}", totalSuccessRevenue);

        if (totalRefundedAmount == null) {
            totalRefundedAmount = BigDecimal.ZERO;
        }
        log.debug("Total refunded amount: {}", totalRefundedAmount);

        // Bước 3.3: Tính tổng doanh thu thực tế = Tổng SUCCESS - Tổng REFUNDED
        BigDecimal totalRevenue = totalSuccessRevenue.subtract(totalRefundedAmount);

        // Đảm bảo totalRevenue không âm
        if (totalRevenue.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Total revenue is negative ({}), setting to 0. This may indicate data inconsistency.",
                    totalRevenue);
            totalRevenue = BigDecimal.ZERO;
        }

        log.info("Final total revenue for merchant {}: {} (SUCCESS: {}, REFUNDED: {})",
                merchantId, totalRevenue, totalSuccessRevenue, totalRefundedAmount);

        // ===== BƯỚC 5: XÁC ĐỊNH PERIOD =====
        // Xác định period dựa trên date range
        String period = determinePeriod(fromDate, toDate);

        // ===== BƯỚC 6: BUILD RESPONSE =====
        PaymentStatisticsResponse response = PaymentStatisticsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .successfulPayments(successfulPayments)
                .failedPayments(failedPayments)
                .pendingPayments(pendingPayments)
                .refundedPayments(refundedPayments)
                .period(period)
                .build();

        log.info("Statistics calculated successfully for merchant {}: totalRevenue={}, totalOrders={}",
                merchantId, totalRevenue, totalOrders);

        return response;
    }

    /**
     * Xác định period dựa trên khoảng thời gian
     * 
     * @param fromDate ngày bắt đầu
     * @param toDate   ngày kết thúc
     * @return "day", "week", "month", "year", hoặc "custom"
     */
    private String determinePeriod(LocalDateTime fromDate, LocalDateTime toDate) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate);

        if (daysBetween <= 1) {
            return "day";
        } else if (daysBetween <= 7) {
            return "week";
        } else if (daysBetween <= 31) {
            return "month";
        } else if (daysBetween <= 365) {
            return "year";
        } else {
            return "custom";
        }
    }
}
