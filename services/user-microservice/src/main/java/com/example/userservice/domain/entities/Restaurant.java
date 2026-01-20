package com.example.userservice.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pure domain entity representing a Restaurant.
 * Contains business logic and domain rules, independent of persistence framework.
 * No JPA annotations - this is a pure business object.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Restaurant {

    private Long id;
    private Long merchantId;
    private String name;
    private String description;
    private String address;
    private String city;
    private String district;

    /**
     * Restaurant GPS coordinates for location-based services (drone pickup, distance calculation)
     */
    private BigDecimal latitude;
    private BigDecimal longitude;

    private String image;
    private String phone;
    private String email;

    /**
     * Opening hours stored as a JSON/string: e.g. {"monday":"08:00-22:00",...}
     */
    private String openingHours;

    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    private Boolean approved = Boolean.FALSE;

    /**
     * Restaurant category: FOOD/DRINK/BOTH/OTHER
     */
    private String category;

    private BigDecimal deliveryFee;
    private Integer estimatedDeliveryTime; // in minutes
    private Double rating;
    private Integer reviewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
