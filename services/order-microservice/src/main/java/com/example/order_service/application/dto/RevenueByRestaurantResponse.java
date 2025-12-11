package com.example.order_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueByRestaurantResponse {
    
    private List<RestaurantRevenue> restaurants;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantRevenue {
        private Long merchantId;
        private String restaurantName;
        private BigDecimal revenue;
    }
}

