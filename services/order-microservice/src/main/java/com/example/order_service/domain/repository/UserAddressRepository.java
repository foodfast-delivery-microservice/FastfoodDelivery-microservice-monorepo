package com.example.order_service.domain.repository;

import com.example.order_service.domain.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserId(Long userId);

    long countBySource(com.example.order_service.domain.model.AddressSource source);
}


