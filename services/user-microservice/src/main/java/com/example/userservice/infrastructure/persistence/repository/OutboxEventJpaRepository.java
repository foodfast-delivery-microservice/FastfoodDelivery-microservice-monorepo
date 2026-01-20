package com.example.userservice.infrastructure.persistence.repository;

import com.example.userservice.domain.valueobjects.EventStatus;
import com.example.userservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository for OutboxEventJpaEntity persistence.
 */
@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {

    List<OutboxEventJpaEntity> findByStatus(EventStatus status);

    @Query("SELECT o FROM OutboxEventJpaEntity o WHERE o.status = :status AND o.createdAt < :cutoffTime")
    List<OutboxEventJpaEntity> findFailedEventsBefore(
            @Param("status") EventStatus status,
            @Param("cutoffTime") LocalDateTime cutoffTime);

    void deleteByStatusAndCreatedAtBefore(EventStatus status, LocalDateTime cutoffTime);
}
