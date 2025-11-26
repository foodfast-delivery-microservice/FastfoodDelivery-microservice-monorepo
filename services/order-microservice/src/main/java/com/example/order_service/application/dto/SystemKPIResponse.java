package com.example.order_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemKPIResponse {

    // Order counts by status
    private Map<String, Long> todayOrdersByStatus;

    // Total orders today
    private Long todayTotalOrders;

    // Pending orders count
    private Long pendingOrdersCount;

    // Recent orders (last 10 minutes)
    private Long recentOrdersCount;

    // Average order value today
    private Double averageOrderValue;
}
