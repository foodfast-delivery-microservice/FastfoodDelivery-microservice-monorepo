package com.example.order_service.application.dto;

/**
 * DTO for Payment Service validation request
 */
public record PaymentValidationRequest(
        Long userId,
        String paymentMethod
) {
}

