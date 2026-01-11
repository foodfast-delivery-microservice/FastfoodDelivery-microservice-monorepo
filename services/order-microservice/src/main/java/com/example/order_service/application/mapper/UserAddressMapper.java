package com.example.order_service.application.mapper;

import com.example.order_service.application.dto.UserAddressResponse;
import com.example.order_service.domain.entities.UserAddress;

public class UserAddressMapper {

    public static UserAddressResponse toResponse(UserAddress userAddress) {
        return UserAddressResponse.builder()
                .id(userAddress.getId())
                .street(userAddress.getStreet())
                .provinceCode(userAddress.getProvinceCode())
                .provinceName(userAddress.getProvinceName())
                .communeCode(userAddress.getCommuneCode())
                .communeName(userAddress.getCommuneName())
                .districtName(userAddress.getDistrictName())
                .fullAddress(userAddress.getFullAddress())
                .note(userAddress.getNote())
                .lat(userAddress.getLat())
                .lng(userAddress.getLng())
                .source(userAddress.getSource())
                .createdAt(userAddress.getCreatedAt())
                .updatedAt(userAddress.getUpdatedAt())
                .build();
    }
}
