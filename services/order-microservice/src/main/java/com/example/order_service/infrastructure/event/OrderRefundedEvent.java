package com.example.order_service.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRefundedEvent {
    private Long orderId;
    private Long userId;
    private Long merchantId;
    private String reason;
    private List<OrderItemRefund> orderItems; // Danh sách sản phẩm cần restore stock

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRefund {
        private Long productId;
        private Integer quantity; // Số lượng cần trả lại vào stock
        private Long merchantId;
    }
}


