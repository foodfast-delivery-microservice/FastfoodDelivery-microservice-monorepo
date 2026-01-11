package com.example.order_service.domain.repository;

import com.example.order_service.domain.entities.UserAddress;
import com.example.order_service.domain.valueobjects.AddressSource;

import java.util.List;
import java.util.Optional;

/**
 * Pure domain repository interface for UserAddress aggregate.
 * No framework dependencies - follows clean architecture principles.
 */
public interface UserAddressRepository {

    // Basic CRUD
    UserAddress save(UserAddress userAddress);

    Optional<UserAddress> findById(Long id);

    List<UserAddress> findAll();

    void deleteById(Long id);

    boolean existsById(Long id);

    long count();

    // Business queries
    List<UserAddress> findByUserId(Long userId);

    long countBySource(AddressSource source);
}
