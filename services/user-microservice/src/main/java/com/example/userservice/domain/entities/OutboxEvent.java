package com.example.userservice.domain.entities;

import com.example.userservice.domain.valueobjects.EventStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Pure domain entity representing an Outbox Event.
 * Used for transactional outbox pattern to ensure reliable event publishing.
 * No JPA annotations - this is a pure business object.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    private Long id;
    private String aggregateType; // e.g., "User", "Merchant"
    private String aggregateId; // e.g., user ID
    private String type; // e.g., "UserUpdated", "MerchantActivated", "MerchantDeactivated"
    private String payload; // JSON payload

    @Builder.Default
    private EventStatus status = EventStatus.NEW;

    private LocalDateTime createdAt;

    // ==================== Business Logic Methods ====================

    /**
     * Mark event as successfully processed
     */
    public void markAsProcessed() {
        if (status == EventStatus.PROCESSED) {
            throw new IllegalStateException("Event is already processed");
        }
        this.status = EventStatus.PROCESSED;
    }

    /**
     * Mark event as failed
     */
    public void markAsFailed() {
        if (status == EventStatus.PROCESSED) {
            throw new IllegalStateException("Cannot mark processed event as failed");
        }
        this.status = EventStatus.FAILED;
    }

    /**
     * Check if event can be retried
     */
    public boolean canBeRetried() {
        return status.canBeReprocessed();
    }

    /**
     * Validate the outbox event
     */
    public void validate() {
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalStateException("Aggregate type cannot be blank");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalStateException("Aggregate ID cannot be blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalStateException("Event type cannot be blank");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalStateException("Payload cannot be blank");
        }
    }
}
