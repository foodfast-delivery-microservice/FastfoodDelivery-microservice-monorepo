package com.example.userservice.infrastructure.persistence.adapter;

import com.example.userservice.domain.entities.Restaurant;
import com.example.userservice.domain.repository.RestaurantRepository;
import com.example.userservice.infrastructure.persistence.entity.RestaurantJpaEntity;
import com.example.userservice.infrastructure.persistence.mapper.RestaurantMapper;
import com.example.userservice.infrastructure.persistence.repository.RestaurantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing domain RestaurantRepository using JPA infrastructure.
 * Bridges the gap between domain layer and infrastructure layer.
 */
@Repository
@RequiredArgsConstructor
public class RestaurantRepositoryImpl implements RestaurantRepository {

    private final RestaurantJpaRepository restaurantJpaRepository;

    @Override
    public Optional<Restaurant> findById(Long id) {
        return restaurantJpaRepository.findById(id)
                .map(RestaurantMapper::toDomainEntity);
    }

    @Override
    public Optional<Restaurant> findByMerchantId(Long merchantId) {
        return restaurantJpaRepository.findByMerchantId(merchantId)
                .map(RestaurantMapper::toDomainEntity);
    }

    @Override
    public List<Restaurant> findByActiveAndApproved(Boolean active, Boolean approved) {
        return restaurantJpaRepository.findByActiveAndApproved(active, approved).stream()
                .map(RestaurantMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Restaurant> findByActiveTrueAndApprovedTrue() {
        return restaurantJpaRepository.findByActiveTrueAndApprovedTrue().stream()
                .map(RestaurantMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Restaurant save(Restaurant restaurant) {
        RestaurantJpaEntity jpaEntity = RestaurantMapper.toJpaEntity(restaurant);
        RestaurantJpaEntity saved = restaurantJpaRepository.save(jpaEntity);
        return RestaurantMapper.toDomainEntity(saved);
    }

    @Override
    public List<Restaurant> findAll() {
        return restaurantJpaRepository.findAll().stream()
                .map(RestaurantMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        restaurantJpaRepository.deleteById(id);
    }
}
