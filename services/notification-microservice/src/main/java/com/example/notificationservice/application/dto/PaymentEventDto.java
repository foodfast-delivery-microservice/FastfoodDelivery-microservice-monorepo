package com.example.notificationservice.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventDto {

    @NotNull(message = "Payment ID cannot be null")
    @Positive(message = "Payment ID must be positive")
    private Long paymentId;

    @NotNull(message = "Order ID cannot be null")
    @Positive(message = "Order ID must be positive")
    private Long orderId;

    @NotNull(message = "User ID cannot be null")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private Instant paymentTime;
    private String transactionId;
    private String status;
    private String failureReason;
}

