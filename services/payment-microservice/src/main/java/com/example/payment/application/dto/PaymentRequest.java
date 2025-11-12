package com.example.payment.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentRequest {
    @NotNull(message = "OrderId is required")
    private Long orderId;

    // userId is optional in request body
    // - For REST API: userId will be extracted from JWT token in PaymentController
    // - For Event Listener: userId comes from order service event
    private Long userId;

    @NotNull(message = "GrandTotal is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal grandTotal;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;
}
