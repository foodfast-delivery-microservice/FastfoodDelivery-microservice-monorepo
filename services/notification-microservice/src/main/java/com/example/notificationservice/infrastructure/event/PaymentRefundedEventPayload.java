package com.example.notificationservice.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundedEventPayload {
    private Long paymentId;
    private Long orderId;
    private String status;
    private String reason;
}
