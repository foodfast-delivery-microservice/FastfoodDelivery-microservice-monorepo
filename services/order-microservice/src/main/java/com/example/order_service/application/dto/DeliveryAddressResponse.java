package com.example.order_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String fullAddress;
}
