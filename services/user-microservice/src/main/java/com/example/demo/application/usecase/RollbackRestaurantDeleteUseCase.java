package com.example.demo.application.usecase;

import com.example.demo.domain.model.Restaurant;
import com.example.demo.domain.model.RestaurantStatus;
import com.example.demo.domain.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case to rollback restaurant deletion when validation fails.
 * Changes status from DELETE_PENDING back to ACTIVE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RollbackRestaurantDeleteUseCase {

    private final RestaurantRepository restaurantRepository;

    @Transactional
    public void execute(Long restaurantId, String reason) {
        log.info("Rolling back restaurant deletion for restaurantId: {}. Reason: {}", restaurantId, reason);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with id: " + restaurantId));

        // Validate current status
        if (restaurant.getStatus() != RestaurantStatus.DELETE_PENDING) {
            log.warn("Restaurant {} is not in DELETE_PENDING status, current status: {}. Skipping rollback.",
                    restaurantId, restaurant.getStatus());
            return; // Idempotent - already rolled back or in different state
        }

        // Rollback to ACTIVE status
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurantRepository.save(restaurant);

        log.info("Restaurant {} status rolled back to ACTIVE. Reason: {}", restaurantId, reason);
    }
}
