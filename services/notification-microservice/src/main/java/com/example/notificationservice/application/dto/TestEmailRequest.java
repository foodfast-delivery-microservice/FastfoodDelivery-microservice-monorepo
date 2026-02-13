package com.example.notificationservice.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestEmailRequest {

    @NotNull(message = "OrderId is required")
    private Long orderId;

    @NotNull(message = "PaymentId is required")
    private Long paymentId;

    @NotNull(message = "UserId is required")
    private Long userId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String transactionId;

    private String failureReason; // Only for failed email
}
