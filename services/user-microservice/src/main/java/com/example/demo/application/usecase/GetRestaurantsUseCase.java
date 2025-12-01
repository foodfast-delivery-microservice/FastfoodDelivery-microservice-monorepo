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
import java.util.stream.Collectors;

public class GetRestaurantsUseCase {
    private final RestaurantRepository restaurantRepository;

    public GetRestaurantsUseCase(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public Page<RestaurantResponse> execute(String name, String category, String city, Pageable pageable) {
        List<Restaurant> restaurants = restaurantRepository.findByActiveTrueAndApprovedTrue();

        List<RestaurantResponse> filtered = restaurants.stream()
                .filter(r -> matchName(r, name))
                .filter(r -> matchCategory(r, category))
                .filter(r -> matchCity(r, city))
                .map(RestaurantResponse::fromEntity)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<RestaurantResponse> pageContent = start > end ? List.of() : filtered.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    private boolean matchName(Restaurant restaurant, String name) {
        if (!StringUtils.hasText(name)) {
            return true;
        }
        return StringUtils.hasText(restaurant.getName()) &&
                restaurant.getName().toLowerCase(Locale.ROOT)
                        .contains(name.toLowerCase(Locale.ROOT));
    }

    private boolean matchCategory(Restaurant restaurant, String category) {
        if (!StringUtils.hasText(category)) {
            return true;
        }
        return StringUtils.hasText(restaurant.getCategory()) &&
                restaurant.getCategory().equalsIgnoreCase(category);
    }

    private boolean matchCity(Restaurant restaurant, String city) {
        if (!StringUtils.hasText(city)) {
            return true;
        }
        return StringUtils.hasText(restaurant.getCity()) &&
                restaurant.getCity().equalsIgnoreCase(city);
    }
}
