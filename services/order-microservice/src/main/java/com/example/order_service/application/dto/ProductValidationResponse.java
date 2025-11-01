package com.example.order_service.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record ProductValidationResponse(
        @JsonProperty("productId")  // ← SỬA: Từ "id" thành "productId"
        Long productId,

        @JsonProperty("success")
        boolean success,

        @JsonProperty("productName")
        String productName,

        @JsonProperty("unitPrice")
        BigDecimal unitPrice
) {
}
