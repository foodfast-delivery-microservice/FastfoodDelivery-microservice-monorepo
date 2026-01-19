package com.example.paymentservice.domain.entities;

import com.example.paymentservice.domain.valueobjects.EventStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Pure domain entity representing an OutboxEvent.
 * Contains business logic and domain rules, independent of persistence framework.
 * No JPA annotations - this is a pure business object.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    private Long id;
    private String aggregateType;
    private String aggregateId;
    private String type;
    private String payload;
    
    @Builder.Default
    private EventStatus status = EventStatus.NEW;
    
    private LocalDateTime createdAt;

    /**
     * Business logic: Mark event as processed
     */
    public void markAsProcessed() {
        this.status = EventStatus.PROCESSED;
    }

    /**
     * Business logic: Mark event as failed
     */
    public void markAsFailed() {
        this.status = EventStatus.FAILED;
    }
}
