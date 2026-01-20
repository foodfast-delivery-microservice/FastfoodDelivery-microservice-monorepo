package com.example.userservice.application.usecases.restaurant;

import com.example.userservice.domain.entities.Restaurant;
import com.example.userservice.domain.repository.RestaurantRepository;
import com.example.userservice.application.DTOs.restaurant.RestaurantDetailResponse;
import com.example.userservice.application.DTOs.restaurant.UpdateRestaurantRequest;
import org.springframework.transaction.annotation.Transactional;

public class UpdateRestaurantUseCase {

    private final RestaurantRepository restaurantRepository;

    public UpdateRestaurantUseCase(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional
    public RestaurantDetailResponse execute(Long merchantId, UpdateRestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findByMerchantId(merchantId)
                .orElseGet(() -> Restaurant.builder()
                        .merchantId(merchantId)
                        .approved(Boolean.FALSE)
                        .active(Boolean.TRUE)
                        .build());

        restaurant.setName(request.getName());
        restaurant.setAddress(request.getAddress());
        restaurant.setCity(request.getCity());
        restaurant.setDistrict(request.getDistrict());
        restaurant.setDescription(request.getDescription());
        restaurant.setImage(request.getImage());
        restaurant.setPhone(request.getPhone());
        restaurant.setEmail(request.getEmail());
        restaurant.setOpeningHours(request.getOpeningHours());
        restaurant.setCategory(request.getCategory());
        restaurant.setDeliveryFee(request.getDeliveryFee());
        restaurant.setEstimatedDeliveryTime(request.getEstimatedDeliveryTime());
        restaurant.setActive(request.getActive());

        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantDetailResponse.fromEntity(saved);
    }
}

