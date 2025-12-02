package com.example.demo.interfaces.rest.dto.restaurant;

import com.example.demo.domain.model.Restaurant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RestaurantDetailResponse extends RestaurantResponse {
    private String description;
    private String phone;
    private String email;

    public static RestaurantDetailResponse fromEntity(Restaurant restaurant) {
        if (restaurant == null) {
            return null;
        }
        RestaurantDetailResponse response = new RestaurantDetailResponse();
        RestaurantResponse base = RestaurantResponse.fromEntity(restaurant);
        response.copyFromBase(base);
        response.setDescription(restaurant.getDescription());
        response.setPhone(restaurant.getPhone());
        response.setEmail(restaurant.getEmail());
        return response;
    }

    private void copyFromBase(RestaurantResponse base) {
        if (base == null) {
            return;
        }
        setId(base.getId());
        setMerchantId(base.getMerchantId());
        setName(base.getName());
        setAddress(base.getAddress());
        setCity(base.getCity());
        setDistrict(base.getDistrict());
        setImage(base.getImage());
        setOpeningHours(base.getOpeningHours());
        setIsOpen(base.getIsOpen());
        setCategory(base.getCategory());
        setDeliveryFee(base.getDeliveryFee());
        setEstimatedDeliveryTime(base.getEstimatedDeliveryTime());
        setRating(base.getRating());
        setReviewCount(base.getReviewCount());
        setActive(base.getActive());
        setApproved(base.getApproved());
        setLatitude(base.getLatitude());
        setLongitude(base.getLongitude());
    }
}







