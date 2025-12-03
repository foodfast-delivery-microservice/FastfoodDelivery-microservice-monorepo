package com.example.droneservice.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddressResponse {
    private String fullAddress;
    private String city;
    private String district;
    private String ward;
    private String street;
    private Double latitude;
    private Double longitude;
}

