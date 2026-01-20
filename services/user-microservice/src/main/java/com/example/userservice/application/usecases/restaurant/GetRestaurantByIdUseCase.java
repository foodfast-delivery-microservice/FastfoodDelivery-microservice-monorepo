package com.example.userservice.application.usecases.restaurant;

import com.example.userservice.domain.exception.ResourceNotFoundException;
import com.example.userservice.domain.entities.Restaurant;
import com.example.userservice.domain.repository.RestaurantRepository;
import com.example.userservice.application.DTOs.restaurant.RestaurantDetailResponse;
import org.springframework.lang.NonNull;
public class GetRestaurantByIdUseCase {

    private final RestaurantRepository restaurantRepository;

    public GetRestaurantByIdUseCase(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public RestaurantDetailResponse execute(@NonNull Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        return RestaurantDetailResponse.fromEntity(restaurant);
    }
}

