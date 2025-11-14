package com.example.payment.application.usecase;

import com.example.payment.application.dto.PaymentStatisticsResponse;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho GetMerchantPaymentStatisticsUseCase
 * 
 * TODO: Hoàn thiện các test cases sau:
 * 1. Test với dữ liệu hợp lệ - các trường hợp khác nhau
 * 2. Test validation merchantId (null, <= 0)
 * 3. Test validation date range (fromDate > toDate, range > 365 days)
 * 4. Test với null values từ repository
 * 5. Test tính toán period (day, week, month, year, custom)
 * 6. Test tính toán revenue với refunded payments
 * 7. Test edge cases (no payments, all refunded, etc.)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetMerchantPaymentStatisticsUseCase Tests")
class GetMerchantPaymentStatisticsUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private GetMerchantPaymentStatisticsUseCase useCase;

    private Long merchantId;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;

    @BeforeEach
    void setUp() {
        merchantId = 1L;
        fromDate = LocalDateTime.now().minusDays(30);
        toDate = LocalDateTime.now();
    }

    @Test
    @DisplayName("Should throw exception when merchantId is null")
    void testExecute_WithNullMerchantId_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(null, fromDate, toDate)
        );
        
        assertEquals("merchantId must be a positive number", exception.getMessage());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Should throw exception when merchantId is zero or negative")
    void testExecute_WithInvalidMerchantId_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> useCase.execute(0L, fromDate, toDate));
        assertThrows(IllegalArgumentException.class, 
                () -> useCase.execute(-1L, fromDate, toDate));
    }

    @Test
    @DisplayName("Should throw exception when fromDate is after toDate")
    void testExecute_WithInvalidDateRange_ShouldThrowException() {
        // Given
        LocalDateTime invalidFromDate = LocalDateTime.now();
        LocalDateTime invalidToDate = LocalDateTime.now().minusDays(1);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(merchantId, invalidFromDate, invalidToDate)
        );
        
        assertEquals("fromDate cannot be after toDate", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when date range exceeds 365 days")
    void testExecute_WithDateRangeExceeding365Days_ShouldThrowException() {
        // Given
        LocalDateTime from = LocalDateTime.now().minusDays(400);
        LocalDateTime to = LocalDateTime.now();

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(merchantId, from, to)
        );
        
        assertEquals("Date range cannot exceed 365 days", exception.getMessage());
    }

    @Test
    @DisplayName("Should use default date range when dates are null")
    void testExecute_WithNullDates_ShouldUseDefaultRange() {
        // Given
        when(paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                anyLong(), any(Payment.Status.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(paymentRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                anyLong(), any(Payment.Status.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        // When
        useCase.execute(merchantId, null, null);

        // Then
        verify(paymentRepository, atLeastOnce()).countByMerchantIdAndStatusAndCreatedAtBetween(
                eq(merchantId), any(Payment.Status.class), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should calculate statistics correctly with valid data")
    void testExecute_WithValidData_ShouldReturnCorrectStatistics() {
        // Given
        Long successfulPayments = 10L;
        Long failedPayments = 2L;
        Long pendingPayments = 3L;
        Long refundedPayments = 1L;
        BigDecimal successRevenue = new BigDecimal("1000000");
        BigDecimal refundedAmount = new BigDecimal("100000");

        when(paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.SUCCESS, fromDate, toDate))
                .thenReturn(successfulPayments);
        when(paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.FAILED, fromDate, toDate))
                .thenReturn(failedPayments);
        when(paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.PENDING, fromDate, toDate))
                .thenReturn(pendingPayments);
        when(paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.REFUNDED, fromDate, toDate))
                .thenReturn(refundedPayments);
        when(paymentRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.SUCCESS, fromDate, toDate))
                .thenReturn(successRevenue);
        when(paymentRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.REFUNDED, fromDate, toDate))
                .thenReturn(refundedAmount);

        // When
        PaymentStatisticsResponse response = useCase.execute(merchantId, fromDate, toDate);

        // Then
        assertNotNull(response);
        assertEquals(new BigDecimal("900000"), response.getTotalRevenue()); // 1000000 - 100000
        assertEquals(16L, response.getTotalOrders()); // 10 + 2 + 3 + 1
        assertEquals(successfulPayments, response.getSuccessfulPayments());
        assertEquals(failedPayments, response.getFailedPayments());
        assertEquals(pendingPayments, response.getPendingPayments());
        assertEquals(refundedPayments, response.getRefundedPayments());
        assertNotNull(response.getPeriod());
    }

    @Test
    @DisplayName("Should handle null values from repository")
    void testExecute_WithNullFromRepository_ShouldUseZero() {
        // Given
        when(paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                anyLong(), any(Payment.Status.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(null);
        when(paymentRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                anyLong(), any(Payment.Status.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(null);

        // When
        PaymentStatisticsResponse response = useCase.execute(merchantId, fromDate, toDate);

        // Then
        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getTotalRevenue());
        assertEquals(0L, response.getTotalOrders());
    }

    @Test
    @DisplayName("Should set period correctly based on date range")
    void testExecute_ShouldSetPeriodCorrectly() {
        // Given
        when(paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                anyLong(), any(Payment.Status.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(paymentRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                anyLong(), any(Payment.Status.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        // Test day period
        LocalDateTime dayFrom = LocalDateTime.now().minusHours(12);
        LocalDateTime dayTo = LocalDateTime.now();
        PaymentStatisticsResponse dayResponse = useCase.execute(merchantId, dayFrom, dayTo);
        assertEquals("day", dayResponse.getPeriod());

        // Test week period
        LocalDateTime weekFrom = LocalDateTime.now().minusDays(5);
        LocalDateTime weekTo = LocalDateTime.now();
        PaymentStatisticsResponse weekResponse = useCase.execute(merchantId, weekFrom, weekTo);
        assertEquals("week", weekResponse.getPeriod());

        // Test month period
        PaymentStatisticsResponse monthResponse = useCase.execute(merchantId, fromDate, toDate);
        assertEquals("month", monthResponse.getPeriod());
    }

    @Test
    @DisplayName("Should handle negative revenue and set to zero")
    void testExecute_WithNegativeRevenue_ShouldSetToZero() {
        // Given - Trường hợp refunded > success (có thể do data inconsistency)
        when(paymentRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                anyLong(), any(Payment.Status.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(paymentRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.SUCCESS, fromDate, toDate))
                .thenReturn(new BigDecimal("100000"));
        when(paymentRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, Payment.Status.REFUNDED, fromDate, toDate))
                .thenReturn(new BigDecimal("200000")); // Refunded > Success

        // When
        PaymentStatisticsResponse response = useCase.execute(merchantId, fromDate, toDate);

        // Then
        assertEquals(BigDecimal.ZERO, response.getTotalRevenue());
    }
}

