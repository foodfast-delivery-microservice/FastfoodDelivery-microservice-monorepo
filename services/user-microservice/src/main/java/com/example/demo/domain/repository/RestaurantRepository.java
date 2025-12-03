package com.example.demo.domain.repository;

import com.example.demo.domain.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByMerchantId(Long merchantId);

    List<Restaurant> findByActiveAndApproved(Boolean active, Boolean approved);

    List<Restaurant> findByActiveTrueAndApprovedTrue();
}










