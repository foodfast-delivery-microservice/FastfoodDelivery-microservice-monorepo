package com.example.order_service.infrastructure.event;

import com.example.order_service.domain.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor // Cần cho việc deserialize (nếu payment-service dùng)
@AllArgsConstructor // Cần cho @Builder
public class OrderCreatedEventPayload {
    private Long orderId;
    private Long userId;
    private BigDecimal grandTotal;
    private String currency;
    private OrderCreatedEventPayload createPayloadFromOrder(Order order) {
        return OrderCreatedEventPayload.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .grandTotal(order.getGrandTotal()) // Lấy tổng tiền sau khi đã calculateTotals()
                .currency(order.getCurrency())
                .build();
    }
}