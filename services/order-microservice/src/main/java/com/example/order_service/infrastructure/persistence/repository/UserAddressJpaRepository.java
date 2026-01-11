package com.example.order_service.infrastructure.persistence.repository;

import com.example.order_service.domain.valueobjects.AddressSource;
import com.example.order_service.infrastructure.persistence.entity.UserAddressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository for UserAddressJpaEntity persistence.
 */
@Repository
public interface UserAddressJpaRepository extends JpaRepository<UserAddressJpaEntity, Long> {

    List<UserAddressJpaEntity> findByUserId(Long userId);

    long countBySource(AddressSource source);
}
