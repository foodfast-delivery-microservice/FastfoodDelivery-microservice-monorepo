package com.example.order_service.infrastructure.event;

import lombok.Data;

@Data
public class PaymentFailedEvent {
    public Long paymentId;
    public Long orderId;
    public String reason;
}
