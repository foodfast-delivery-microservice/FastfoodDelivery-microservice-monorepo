package com.example.userservice.infrastructure.persistence.repository;

import com.example.userservice.domain.entities.User;
import com.example.userservice.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for UserJpaEntity persistence.
 * This is the infrastructure layer repository with Spring Data JPA.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    
    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<UserJpaEntity> findByRoleAndActive(User.UserRole role, boolean active);
}
