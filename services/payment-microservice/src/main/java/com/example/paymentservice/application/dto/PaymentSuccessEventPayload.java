package com.example.paymentservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEventPayload {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private String transactionNo;
}
