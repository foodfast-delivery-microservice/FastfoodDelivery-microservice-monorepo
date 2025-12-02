package com.example.order_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressMetricsResponse {
    private long total;
    private long geocodeOnly;
    private long userAdjust;
    private long driverAdjust;
}



