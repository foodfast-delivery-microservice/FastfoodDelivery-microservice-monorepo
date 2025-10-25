package com.example.order_service.domain.repository;

import com.example.order_service.domain.model.EventStatus;
import com.example.order_service.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatus(EventStatus status);

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status AND o.createdAt < :cutoffTime")
    List<OutboxEvent> findFailedEventsBefore(@Param("status") EventStatus status,
                                             @Param("cutoffTime") LocalDateTime cutoffTime);

    void deleteByStatusAndCreatedAtBefore(EventStatus status, LocalDateTime cutoffTime);
}
