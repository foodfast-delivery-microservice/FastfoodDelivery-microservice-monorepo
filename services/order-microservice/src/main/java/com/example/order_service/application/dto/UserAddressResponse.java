package com.example.order_service.application.dto;

import com.example.order_service.domain.model.AddressSource;
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
public class UserAddressResponse {
    private Long id;
    private String street;
    private String provinceCode;
    private String provinceName;
    private String communeCode;
    private String communeName;
    private String districtName;
    private String fullAddress;
    private String note;
    private BigDecimal lat;
    private BigDecimal lng;
    private AddressSource source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



