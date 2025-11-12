package com.example.demo.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductValidationResponse {
    @JsonProperty("productId")
    private Long productId;
    private boolean isSuccess;
    private String productName;
    private BigDecimal unitPrice;
    private Long merchantId;
    private String message;
}
