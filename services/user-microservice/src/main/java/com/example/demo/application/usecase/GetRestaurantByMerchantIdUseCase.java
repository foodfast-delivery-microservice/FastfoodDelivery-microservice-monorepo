package com.example.demo.application.usecase;

import com.example.demo.domain.exception.ResourceNotFoundException;
import com.example.demo.domain.model.Restaurant;
import com.example.demo.domain.repository.RestaurantRepository;
import com.example.demo.interfaces.rest.dto.restaurant.RestaurantDetailResponse;
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

