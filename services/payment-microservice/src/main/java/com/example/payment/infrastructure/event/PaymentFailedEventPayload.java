package com.example.payment.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEventPayload {
    private Long paymentId;
    private Long orderId;
    private String reason;
}

