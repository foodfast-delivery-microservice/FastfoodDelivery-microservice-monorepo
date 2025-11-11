package com.example.order_service.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Payment Service validation response
 */
public record PaymentValidationResponse(
        @JsonProperty("userId")
        Long userId,
        
        @JsonProperty("paymentMethod")
        String paymentMethod,
        
        @JsonProperty("valid")
        boolean valid,
        
        @JsonProperty("reason")
        String reason
) {
}

