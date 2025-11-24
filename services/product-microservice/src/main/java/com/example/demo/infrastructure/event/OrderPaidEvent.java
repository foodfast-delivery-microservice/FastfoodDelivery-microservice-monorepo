package com.example.demo.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidEvent {
    private Long orderId;
    private Long userId;
    private Long merchantId;
    private List<OrderItemDeduction> orderItems; // Danh sách sản phẩm cần trừ stock

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDeduction {
        private Long productId;
        private Integer quantity; // Số lượng cần trừ từ stock
        private Long merchantId;
    }
}


