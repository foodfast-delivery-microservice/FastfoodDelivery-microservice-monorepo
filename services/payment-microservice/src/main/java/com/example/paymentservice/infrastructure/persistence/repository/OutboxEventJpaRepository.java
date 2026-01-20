package com.example.paymentservice.infrastructure.persistence.repository;

import com.example.paymentservice.domain.valueobjects.EventStatus;
import com.example.paymentservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {
    
    List<OutboxEventJpaEntity> findByStatus(EventStatus status);
}
