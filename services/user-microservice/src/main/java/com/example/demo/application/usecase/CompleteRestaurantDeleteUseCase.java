package com.example.demo.application.usecase;

import com.example.demo.domain.model.Restaurant;
import com.example.demo.domain.model.RestaurantStatus;
import com.example.demo.domain.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case to complete restaurant deletion after validation passes.
 * Changes status from DELETE_PENDING to DELETED (soft delete).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompleteRestaurantDeleteUseCase {

    private final RestaurantRepository restaurantRepository;

    @Transactional
    public void execute(Long restaurantId) {
        log.info("Completing restaurant deletion for restaurantId: {}", restaurantId);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with id: " + restaurantId));

        // Validate current status
        if (restaurant.getStatus() != RestaurantStatus.DELETE_PENDING) {
            log.warn("Restaurant {} is not in DELETE_PENDING status, current status: {}",
                    restaurantId, restaurant.getStatus());
            throw new IllegalStateException("Restaurant is not pending deletion");
        }

        // Set status to DELETED (soft delete)
        restaurant.setStatus(RestaurantStatus.DELETED);
        restaurant.setActive(false);
        restaurantRepository.save(restaurant);

        log.info("Restaurant {} successfully marked as DELETED", restaurantId);

        // Alternative: Hard delete
        // restaurantRepository.delete(restaurant);
        // log.info("Restaurant {} successfully deleted from database", restaurantId);
    }
}
