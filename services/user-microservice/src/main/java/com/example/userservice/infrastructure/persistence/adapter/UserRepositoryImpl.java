package com.example.userservice.infrastructure.persistence.adapter;

import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.infrastructure.persistence.entity.UserJpaEntity;
import com.example.userservice.infrastructure.persistence.mapper.UserMapper;
import com.example.userservice.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing domain UserRepository using JPA infrastructure.
 * Bridges the gap between domain layer and infrastructure layer.
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id)
                .map(UserMapper::toDomainEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(UserMapper::toDomainEntity);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userJpaRepository.findByUsername(username)
                .map(UserMapper::toDomainEntity);
    }

    @Override
    public User save(User user) {
        UserJpaEntity jpaEntity = UserMapper.toJpaEntity(user);
        UserJpaEntity saved = userJpaRepository.save(jpaEntity);
        return UserMapper.toDomainEntity(saved);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public List<User> findByRoleAndActive(User.UserRole role, boolean active) {
        return userJpaRepository.findByRoleAndActive(role, active).stream()
                .map(UserMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll().stream()
                .map(UserMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        userJpaRepository.deleteById(id);
    }
}
