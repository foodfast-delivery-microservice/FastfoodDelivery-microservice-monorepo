package com.example.demo.application.usecase;

import com.example.demo.domain.exception.ResourceNotFoundException;
import com.example.demo.domain.model.Restaurant;
import com.example.demo.domain.repository.RestaurantRepository;
import com.example.demo.interfaces.rest.dto.restaurant.RestaurantDetailResponse;
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

