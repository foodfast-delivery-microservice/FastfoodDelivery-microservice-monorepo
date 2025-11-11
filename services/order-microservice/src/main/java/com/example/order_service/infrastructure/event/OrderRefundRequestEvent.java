package com.example.order_service.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRefundRequestEvent {
    private Long orderId;
    private Long paymentId;
    private BigDecimal refundAmount;
    private String reason;
}
