package com.example.order_service.domain.repository;

import com.example.order_service.domain.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByUserIdAndIdemKey(Long userId, String idemKey);

    boolean existsByUserIdAndIdemKey(Long userId, String idemKey);
}
