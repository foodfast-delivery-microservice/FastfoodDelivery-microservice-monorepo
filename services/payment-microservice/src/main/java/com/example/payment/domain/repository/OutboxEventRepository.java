package com.example.payment.domain.repository;

import com.example.payment.domain.model.EventStatus;
import com.example.payment.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // Hàm này rất quan trọng để OutboxEventRelay tìm event
    List<OutboxEvent> findByStatus(EventStatus status);
}
