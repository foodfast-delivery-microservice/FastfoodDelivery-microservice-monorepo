package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.RevenueByRestaurantResponse;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetRevenueByRestaurantUseCase {

    private final OrderRepository orderRepository;

    public RevenueByRestaurantResponse execute(LocalDateTime fromDate, LocalDateTime toDate) {
        log.info("Getting revenue by restaurant from {} to {}", fromDate, toDate);

        try {
            List<Object[]> results;
            
            // Convert OrderStatus enum to String for native query
            String statusStr = OrderStatus.DELIVERED.name();
            
            if (fromDate != null && toDate != null) {
                results = orderRepository.sumRevenueByMerchantIdAndStatusAndDateRange(
                        statusStr, fromDate, toDate);
            } else {
                results = orderRepository.sumRevenueByMerchantIdAndStatus(statusStr);
            }

            List<RevenueByRestaurantResponse.RestaurantRevenue> restaurantRevenues = results.stream()
                    .map(result -> {
                        try {
                            Long merchantId = null;
                            BigDecimal revenue = BigDecimal.ZERO;
                            
                            // Parse merchantId (first column)
                            if (result[0] != null) {
                                if (result[0] instanceof Number) {
                                    merchantId = ((Number) result[0]).longValue();
                                } else if (result[0] instanceof String) {
                                    merchantId = Long.parseLong((String) result[0]);
                                }
                            }
                            
                            // Parse revenue (second column) - native query returns as BigDecimal or Double
                            if (result[1] != null) {
                                if (result[1] instanceof BigDecimal) {
                                    revenue = (BigDecimal) result[1];
                                } else if (result[1] instanceof Double) {
                                    revenue = BigDecimal.valueOf((Double) result[1]);
                                } else if (result[1] instanceof Number) {
                                    revenue = BigDecimal.valueOf(((Number) result[1]).doubleValue());
                                } else if (result[1] instanceof String) {
                                    revenue = new BigDecimal((String) result[1]);
                                }
                            }
                            
                            if (merchantId == null) {
                                log.warn("Skipping result with null merchantId: {}", java.util.Arrays.toString(result));
                                return null;
                            }
                            
                            return RevenueByRestaurantResponse.RestaurantRevenue.builder()
                                    .merchantId(merchantId)
                                    .restaurantName(null) // Frontend will map with restaurant list
                                    .revenue(revenue)
                                    .build();
                        } catch (Exception e) {
                            log.error("Error mapping revenue result: {}, result: {}", e.getMessage(), java.util.Arrays.toString(result), e);
                            return null;
                        }
                    })
                    .filter(item -> item != null && item.getMerchantId() != null)
                    .collect(Collectors.toList());

            log.info("Found {} restaurants with revenue", restaurantRevenues.size());

            return RevenueByRestaurantResponse.builder()
                    .restaurants(restaurantRevenues)
                    .build();
        } catch (Exception e) {
            log.error("Error getting revenue by restaurant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get revenue by restaurant: " + e.getMessage(), e);
        }
    }
}

