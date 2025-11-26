package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.SystemKPIResponse;
import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetSystemKPIUseCase {

    private final OrderRepository orderRepository;

    public SystemKPIResponse execute(LocalDate date) {
        // Default to today if no date provided
        if (date == null) {
            date = LocalDate.now();
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);

        log.info("Getting system KPIs for date: {}", date);

        // Get all orders for the day
        List<Order> todayOrders = orderRepository.findByCreatedAtBetween(startOfDay, endOfDay);

        // Count orders by status
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = todayOrders.stream()
                    .filter(order -> order.getStatus() == status)
                    .count();
            ordersByStatus.put(status.name(), count);
        }

        // Total orders today
        long totalOrders = todayOrders.size();

        // Pending orders count (system-wide, not just today)
        long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);

        // Recent orders (last 10 minutes)
        long recentCount = orderRepository.countByCreatedAtAfter(tenMinutesAgo);

        // Average order value today
        double avgValue = todayOrders.stream()
                .filter(order -> order.getGrandTotal() != null)
                .mapToDouble(order -> order.getGrandTotal().doubleValue())
                .average()
                .orElse(0.0);

        SystemKPIResponse response = SystemKPIResponse.builder()
                .todayOrdersByStatus(ordersByStatus)
                .todayTotalOrders(totalOrders)
                .pendingOrdersCount(pendingCount)
                .recentOrdersCount(recentCount)
                .averageOrderValue(avgValue)
                .build();

        log.info("System KPIs calculated: total={}, pending={}, recent={}",
                totalOrders, pendingCount, recentCount);

        return response;
    }
}
