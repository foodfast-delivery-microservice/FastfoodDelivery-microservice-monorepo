package com.example.userservice.infrastructure.persistence.repository;

import com.example.userservice.infrastructure.persistence.entity.RestaurantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for RestaurantJpaEntity persistence.
 * This is the infrastructure layer repository with Spring Data JPA.
 */
@Repository
public interface RestaurantJpaRepository extends JpaRepository<RestaurantJpaEntity, Long> {

    Optional<RestaurantJpaEntity> findByMerchantId(Long merchantId);

    List<RestaurantJpaEntity> findByActiveAndApproved(Boolean active, Boolean approved);

    List<RestaurantJpaEntity> findByActiveTrueAndApprovedTrue();
}
