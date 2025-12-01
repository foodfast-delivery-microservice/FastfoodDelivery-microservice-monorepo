package com.example.order_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommuneResponse {
    private String code;
    private String name;
    private String districtName;
    private String provinceCode;
    private String provinceName;
}



