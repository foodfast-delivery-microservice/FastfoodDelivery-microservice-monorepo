package com.example.demo.application.usecase;

import com.example.demo.domain.model.Restaurant;
import com.example.demo.domain.model.RestaurantStatus;
import com.example.demo.domain.repository.RestaurantRepository;
import com.example.demo.infrastructure.messaging.EventPublisher;
import com.example.demo.infrastructure.messaging.event.RestaurantDeleteInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Use case to initiate restaurant deletion using Saga pattern.
 * Sets restaurant status to DELETE_PENDING and publishes event for validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InitiateRestaurantDeleteUseCase {

    private final RestaurantRepository restaurantRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void execute(Long restaurantId) {
        log.info("Initiating restaurant deletion for restaurantId: {}", restaurantId);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with id: " + restaurantId));

        // Validate current status
        if (restaurant.getStatus() == RestaurantStatus.DELETE_PENDING) {
            log.warn("Restaurant {} is already in DELETE_PENDING status", restaurantId);
            throw new IllegalStateException("Restaurant deletion is already in progress");
        }

        if (restaurant.getStatus() == RestaurantStatus.DELETED) {
            log.warn("Restaurant {} is already DELETED", restaurantId);
            throw new IllegalStateException("Restaurant is already deleted");
        }

        // Set status to DELETE_PENDING
        restaurant.setStatus(RestaurantStatus.DELETE_PENDING);
        restaurantRepository.save(restaurant);

        log.info("Restaurant {} status changed to DELETE_PENDING", restaurantId);

        // Publish event to trigger validation in Order Service
        RestaurantDeleteInitiatedEvent event = RestaurantDeleteInitiatedEvent.builder()
                .restaurantId(restaurantId)
                .merchantId(restaurant.getMerchantId())
                .occurredAt(Instant.now())
                .build();

        eventPublisher.publishRestaurantDeleteInitiated(event);
        log.info("Published RestaurantDeleteInitiatedEvent for restaurant {}", restaurantId);
    }
}
