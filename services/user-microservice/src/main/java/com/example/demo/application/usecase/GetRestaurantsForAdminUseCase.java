package com.example.demo.application.usecase;

import com.example.demo.domain.model.Restaurant;
import com.example.demo.domain.repository.RestaurantRepository;
import com.example.demo.interfaces.rest.dto.restaurant.RestaurantResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

// UseCase này dành riêng cho Admin (Xem tất cả Active/Inactive)
public class GetRestaurantsForAdminUseCase {
    private final RestaurantRepository restaurantRepository;

    public GetRestaurantsForAdminUseCase(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public Page<Restaurant> excute(String keyword, String city, String category, Boolean active, Pageable pageable) {
        return restaurantRepository.findWithFilters(keyword, city, category, active, pageable);
    }
}