package com.example.paymentservice.infrastructure.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderRefundRequestEvent {
    private Long orderId;
    private Long paymentId;
    private BigDecimal refundAmount;
    private String reason;
}
