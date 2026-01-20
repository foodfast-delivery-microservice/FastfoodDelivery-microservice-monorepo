package com.example.userservice.application.usecases.restaurant;

import com.example.userservice.domain.exception.ResourceNotFoundException;
import com.example.userservice.domain.entities.Restaurant;
import com.example.userservice.domain.repository.RestaurantRepository;
import com.example.userservice.application.DTOs.restaurant.RestaurantDetailResponse;
public class GetRestaurantByMerchantIdUseCase {

    private final RestaurantRepository restaurantRepository;

    public GetRestaurantByMerchantIdUseCase(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public RestaurantDetailResponse execute(Long merchantId) {
        Restaurant restaurant = restaurantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant for merchant not found"));
        return RestaurantDetailResponse.fromEntity(restaurant);
    }
}

