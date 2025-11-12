package com.example.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String transactionNo;
    private String message;
    private String failReason;
    private LocalDateTime timestamp;
}
