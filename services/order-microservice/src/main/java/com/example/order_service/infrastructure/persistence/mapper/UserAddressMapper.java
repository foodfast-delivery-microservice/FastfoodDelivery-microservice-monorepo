package com.example.order_service.infrastructure.persistence.mapper;

import com.example.order_service.domain.entities.UserAddress;
import com.example.order_service.infrastructure.persistence.entity.UserAddressJpaEntity;

/**
 * Mapper to convert between UserAddress domain entity and UserAddressJpaEntity.
 */
public class UserAddressMapper {

    /**
     * Convert domain entity to JPA entity
     */
    public static UserAddressJpaEntity toJpaEntity(UserAddress domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        UserAddressJpaEntity jpaEntity = new UserAddressJpaEntity();
        jpaEntity.setId(domainEntity.getId());
        jpaEntity.setUserId(domainEntity.getUserId());
        jpaEntity.setStreet(domainEntity.getStreet());
        jpaEntity.setProvinceCode(domainEntity.getProvinceCode());
        jpaEntity.setProvinceName(domainEntity.getProvinceName());
        jpaEntity.setCommuneCode(domainEntity.getCommuneCode());
        jpaEntity.setCommuneName(domainEntity.getCommuneName());
        jpaEntity.setDistrictName(domainEntity.getDistrictName());
        jpaEntity.setFullAddress(domainEntity.getFullAddress());
        jpaEntity.setNote(domainEntity.getNote());
        jpaEntity.setLat(domainEntity.getLat());
        jpaEntity.setLng(domainEntity.getLng());
        jpaEntity.setSource(domainEntity.getSource());
        jpaEntity.setCreatedAt(domainEntity.getCreatedAt());
        jpaEntity.setUpdatedAt(domainEntity.getUpdatedAt());

        return jpaEntity;
    }

    /**
     * Convert JPA entity to domain entity
     */
    public static UserAddress toDomainEntity(UserAddressJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        UserAddress domainEntity = UserAddress.builder()
                .id(jpaEntity.getId())
                .userId(jpaEntity.getUserId())
                .street(jpaEntity.getStreet())
                .provinceCode(jpaEntity.getProvinceCode())
                .provinceName(jpaEntity.getProvinceName())
                .communeCode(jpaEntity.getCommuneCode())
                .communeName(jpaEntity.getCommuneName())
                .districtName(jpaEntity.getDistrictName())
                .fullAddress(jpaEntity.getFullAddress())
                .note(jpaEntity.getNote())
                .lat(jpaEntity.getLat())
                .lng(jpaEntity.getLng())
                .source(jpaEntity.getSource())
                .createdAt(jpaEntity.getCreatedAt())
                .updatedAt(jpaEntity.getUpdatedAt())
                .build();

        return domainEntity;
    }
}
