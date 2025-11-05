package com.example.order_service.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ProductValidationWrapper {

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private List<ProductValidationResponse> data;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("timestamp")
    private String timestamp;
}