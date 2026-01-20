package com.example.userservice.domain.repository;

import com.example.userservice.domain.entities.User;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for User.
 * This is a pure domain interface with no framework dependencies.
 * Implementations are in the infrastructure layer.
 */
public interface UserRepository {
    
    Optional<User> findById(Long id);
    
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    User save(User user);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByRoleAndActive(User.UserRole role, boolean active);
    
    List<User> findAll();
    
    void deleteById(Long id);
}
