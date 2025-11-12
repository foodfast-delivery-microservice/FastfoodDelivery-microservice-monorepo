package com.example.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatisticsResponse {
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long successfulPayments;
    private Long failedPayments;
    private Long pendingPayments;
    private Long refundedPayments;
    private String period; // Optional: "day", "week", "month"
}

