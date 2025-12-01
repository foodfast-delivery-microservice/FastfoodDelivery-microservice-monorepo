package com.example.order_service.application.mapper;

import com.example.order_service.application.dto.UserAddressResponse;
import com.example.order_service.domain.model.UserAddress;

public final class UserAddressMapper {

    private UserAddressMapper() {
    }

    public static UserAddressResponse toResponse(UserAddress entity) {
        if (entity == null) {
            return null;
        }
        return UserAddressResponse.builder()
                .id(entity.getId())
                .street(entity.getStreet())
                .provinceCode(entity.getProvinceCode())
                .provinceName(entity.getProvinceName())
                .communeCode(entity.getCommuneCode())
                .communeName(entity.getCommuneName())
                .districtName(entity.getDistrictName())
                .fullAddress(entity.getFullAddress())
                .note(entity.getNote())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .source(entity.getSource())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}



