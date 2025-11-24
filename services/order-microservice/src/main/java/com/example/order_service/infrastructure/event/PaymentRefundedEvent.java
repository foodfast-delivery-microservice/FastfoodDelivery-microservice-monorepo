package com.example.order_service.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRefundedEvent {
    private Long paymentId;
    private Long orderId;
    private String status;
    private String reason;
}





