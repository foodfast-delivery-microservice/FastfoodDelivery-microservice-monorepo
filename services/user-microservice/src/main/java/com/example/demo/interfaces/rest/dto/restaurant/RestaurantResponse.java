package com.example.demo.interfaces.rest.dto.restaurant;

import com.example.demo.domain.model.Restaurant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantResponse {
    private Long id;
    private Long merchantId;
    private String name;
    private String address;
    private String city;
    private String district;
    private String image;
    private String openingHours;
    private Boolean isOpen;
    private String category;
    private BigDecimal deliveryFee;
    private Integer estimatedDeliveryTime;
    private Double rating;
    private Integer reviewCount;
    private Boolean active;
    private Boolean approved;

    public static RestaurantResponse fromEntity(Restaurant restaurant) {
        if (restaurant == null) {
            return null;
        }

        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .merchantId(restaurant.getMerchantId())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .city(restaurant.getCity())
                .district(restaurant.getDistrict())
                .image(restaurant.getImage())
                .openingHours(restaurant.getOpeningHours())
                .isOpen(calculateIsOpen(restaurant.getOpeningHours()))
                .category(restaurant.getCategory())
                .deliveryFee(restaurant.getDeliveryFee())
                .estimatedDeliveryTime(restaurant.getEstimatedDeliveryTime())
                .rating(Optional.ofNullable(restaurant.getRating()).orElse(0.0))
                .reviewCount(Optional.ofNullable(restaurant.getReviewCount()).orElse(0))
                .active(restaurant.getActive())
                .approved(restaurant.getApproved())
                .build();
    }

    private static boolean calculateIsOpen(String openingHours) {
        // TODO: implement real opening hour calculation
        return openingHours != null && !openingHours.isBlank();
    }
}






