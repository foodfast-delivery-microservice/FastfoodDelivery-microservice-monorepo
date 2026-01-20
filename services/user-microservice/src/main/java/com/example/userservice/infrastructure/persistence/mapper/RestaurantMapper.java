package com.example.userservice.infrastructure.persistence.mapper;

import com.example.userservice.domain.entities.Restaurant;
import com.example.userservice.infrastructure.persistence.entity.RestaurantJpaEntity;

/**
 * Mapper to convert between Restaurant domain entity and RestaurantJpaEntity.
 */
public class RestaurantMapper {

    /**
     * Convert domain entity to JPA entity
     */
    public static RestaurantJpaEntity toJpaEntity(Restaurant domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        RestaurantJpaEntity jpaEntity = new RestaurantJpaEntity();
        jpaEntity.setId(domainEntity.getId());
        jpaEntity.setMerchantId(domainEntity.getMerchantId());
        jpaEntity.setName(domainEntity.getName());
        jpaEntity.setDescription(domainEntity.getDescription());
        jpaEntity.setAddress(domainEntity.getAddress());
        jpaEntity.setCity(domainEntity.getCity());
        jpaEntity.setDistrict(domainEntity.getDistrict());
        jpaEntity.setLatitude(domainEntity.getLatitude());
        jpaEntity.setLongitude(domainEntity.getLongitude());
        jpaEntity.setImage(domainEntity.getImage());
        jpaEntity.setPhone(domainEntity.getPhone());
        jpaEntity.setEmail(domainEntity.getEmail());
        jpaEntity.setOpeningHours(domainEntity.getOpeningHours());
        jpaEntity.setActive(domainEntity.getActive());
        jpaEntity.setApproved(domainEntity.getApproved());
        jpaEntity.setCategory(domainEntity.getCategory());
        jpaEntity.setDeliveryFee(domainEntity.getDeliveryFee());
        jpaEntity.setEstimatedDeliveryTime(domainEntity.getEstimatedDeliveryTime());
        jpaEntity.setRating(domainEntity.getRating());
        jpaEntity.setReviewCount(domainEntity.getReviewCount());
        jpaEntity.setCreatedAt(domainEntity.getCreatedAt());
        jpaEntity.setUpdatedAt(domainEntity.getUpdatedAt());

        return jpaEntity;
    }

    /**
     * Convert JPA entity to domain entity
     */
    public static Restaurant toDomainEntity(RestaurantJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        Restaurant domainEntity = Restaurant.builder()
                .id(jpaEntity.getId())
                .merchantId(jpaEntity.getMerchantId())
                .name(jpaEntity.getName())
                .description(jpaEntity.getDescription())
                .address(jpaEntity.getAddress())
                .city(jpaEntity.getCity())
                .district(jpaEntity.getDistrict())
                .latitude(jpaEntity.getLatitude())
                .longitude(jpaEntity.getLongitude())
                .image(jpaEntity.getImage())
                .phone(jpaEntity.getPhone())
                .email(jpaEntity.getEmail())
                .openingHours(jpaEntity.getOpeningHours())
                .active(jpaEntity.getActive())
                .approved(jpaEntity.getApproved())
                .category(jpaEntity.getCategory())
                .deliveryFee(jpaEntity.getDeliveryFee())
                .estimatedDeliveryTime(jpaEntity.getEstimatedDeliveryTime())
                .rating(jpaEntity.getRating())
                .reviewCount(jpaEntity.getReviewCount())
                .createdAt(jpaEntity.getCreatedAt())
                .updatedAt(jpaEntity.getUpdatedAt())
                .build();

        return domainEntity;
    }
}
