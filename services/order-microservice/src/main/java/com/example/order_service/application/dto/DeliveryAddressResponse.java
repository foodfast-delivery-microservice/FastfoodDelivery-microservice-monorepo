package com.example.order_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddressResponse {

    private String receiverName;
    private String receiverPhone;
    private String addressLine1;
    private String ward;
    private String district;
    private String city;

    // Normalized administrative fields (if available)
    private String provinceCode;
    private String provinceName;
    private String communeCode;
    private String communeName;
    private String normalizedDistrictName;

    private String fullAddress;

    // GPS coordinates (already stored in DeliveryAddress entity)
    private BigDecimal lat;
    private BigDecimal lng;
}
