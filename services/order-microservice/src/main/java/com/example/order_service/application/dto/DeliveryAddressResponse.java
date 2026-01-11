package com.example.order_service.application.dto;

import com.example.order_service.domain.valueobjects.DeliveryAddress;
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

    // Normalized administrative fields (if available)
    private String provinceCode;
    private String provinceName;
    private String communeCode;
    private String communeName;
    private String normalizedDistrictName;

    private String fullAddress;

    /**
     * Static factory method to create response from domain value object
     */
    public static DeliveryAddressResponse fromEntity(DeliveryAddress deliveryAddress) {
        if (deliveryAddress == null) {
            return null;
        }

        return DeliveryAddressResponse.builder()
                .receiverName(deliveryAddress.getReceiverName())
                .receiverPhone(deliveryAddress.getReceiverPhone())
                .addressLine1(deliveryAddress.getAddressLine1())
                .ward(deliveryAddress.getWard())
                .district(deliveryAddress.getDistrict())
                .city(deliveryAddress.getCity())
                .provinceCode(deliveryAddress.getProvinceCode())
                .provinceName(deliveryAddress.getProvinceName())
                .communeCode(deliveryAddress.getCommuneCode())
                .communeName(deliveryAddress.getCommuneName())
                .normalizedDistrictName(deliveryAddress.getNormalizedDistrictName())
                .fullAddress(deliveryAddress.getFullAddress())
                .build();
    }
}
