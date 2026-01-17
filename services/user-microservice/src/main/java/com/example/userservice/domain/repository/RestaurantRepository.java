package com.example.userservice.domain.repository;

import com.example.userservice.domain.entities.Restaurant;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for Restaurant.
 * This is a pure domain interface with no framework dependencies.
 * Implementations are in the infrastructure layer.
 */
public interface RestaurantRepository {

    Optional<Restaurant> findById(Long id);
    
    Optional<Restaurant> findByMerchantId(Long merchantId);

    List<Restaurant> findByActiveAndApproved(Boolean active, Boolean approved);

    List<Restaurant> findByActiveTrueAndApprovedTrue();
    
    Restaurant save(Restaurant restaurant);
    
    List<Restaurant> findAll();
    
    void deleteById(Long id);
}







