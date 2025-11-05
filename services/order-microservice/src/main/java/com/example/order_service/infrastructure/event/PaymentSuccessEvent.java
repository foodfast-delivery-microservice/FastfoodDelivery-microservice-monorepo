package com.example.order_service.infrastructure.event;

import lombok.Data;

@Data
public class PaymentSuccessEvent {
    private Long paymentId;
    private Long orderId;
}