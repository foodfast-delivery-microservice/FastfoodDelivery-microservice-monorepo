package com.example.order_service.infrastructure.event;

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
    private Long merchantId; // Thêm merchantId để payment service không cần gọi lại order service
    private BigDecimal grandTotal;
    private String currency;
}