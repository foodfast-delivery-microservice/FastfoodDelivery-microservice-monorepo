package com.example.order_service.infrastructure.persistence.mapper;

import com.example.order_service.domain.valueobjects.DeliveryAddress;
import com.example.order_service.infrastructure.persistence.entity.DeliveryAddressEmbeddable;

/**
 * Mapper to convert between DeliveryAddress value object and
 * DeliveryAddressEmbeddable JPA entity.
 */
public class DeliveryAddressMapper {

    /**
     * Convert domain value object to JPA embeddable
     */
    public static DeliveryAddressEmbeddable toEmbeddable(DeliveryAddress valueObject) {
        if (valueObject == null) {
            return null;
        }

        return DeliveryAddressEmbeddable.builder()
                .receiverName(valueObject.getReceiverName())
                .receiverPhone(valueObject.getReceiverPhone())
                .addressLine1(valueObject.getAddressLine1())
                .ward(valueObject.getWard())
                .district(valueObject.getDistrict())
                .city(valueObject.getCity())
                .provinceCode(valueObject.getProvinceCode())
                .provinceName(valueObject.getProvinceName())
                .communeCode(valueObject.getCommuneCode())
                .communeName(valueObject.getCommuneName())
                .normalizedDistrictName(valueObject.getNormalizedDistrictName())
                .lat(valueObject.getLat())
                .lng(valueObject.getLng())
                .build();
    }

    /**
     * Convert JPA embeddable to domain value object
     */
    public static DeliveryAddress toValueObject(DeliveryAddressEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }

        return DeliveryAddress.builder()
                .receiverName(embeddable.getReceiverName())
                .receiverPhone(embeddable.getReceiverPhone())
                .addressLine1(embeddable.getAddressLine1())
                .ward(embeddable.getWard())
                .district(embeddable.getDistrict())
                .city(embeddable.getCity())
                .provinceCode(embeddable.getProvinceCode())
                .provinceName(embeddable.getProvinceName())
                .communeCode(embeddable.getCommuneCode())
                .communeName(embeddable.getCommuneName())
                .normalizedDistrictName(embeddable.getNormalizedDistrictName())
                .lat(embeddable.getLat())
                .lng(embeddable.getLng())
                .build();
    }
}
