package com.example.order_service.infrastructure.persistence.adapter;

import com.example.order_service.domain.entities.UserAddress;
import com.example.order_service.domain.repository.UserAddressRepository;
import com.example.order_service.domain.valueobjects.AddressSource;
import com.example.order_service.infrastructure.persistence.mapper.UserAddressMapper;
import com.example.order_service.infrastructure.persistence.repository.UserAddressJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing domain UserAddressRepository using JPA infrastructure.
 */
@Repository
@RequiredArgsConstructor
public class UserAddressRepositoryImpl implements UserAddressRepository {

    private final UserAddressJpaRepository userAddressJpaRepository;

    @Override
    public UserAddress save(UserAddress userAddress) {
        var jpaEntity = UserAddressMapper.toJpaEntity(userAddress);
        var savedEntity = userAddressJpaRepository.save(jpaEntity);
        return UserAddressMapper.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<UserAddress> findById(Long id) {
        return userAddressJpaRepository.findById(id)
                .map(UserAddressMapper::toDomainEntity);
    }

    @Override
    public List<UserAddress> findAll() {
        return userAddressJpaRepository.findAll().stream()
                .map(UserAddressMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        userAddressJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return userAddressJpaRepository.existsById(id);
    }

    @Override
    public long count() {
        return userAddressJpaRepository.count();
    }

    @Override
    public List<UserAddress> findByUserId(Long userId) {
        return userAddressJpaRepository.findByUserId(userId).stream()
                .map(UserAddressMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countBySource(AddressSource source) {
        return userAddressJpaRepository.countBySource(source);
    }
}
