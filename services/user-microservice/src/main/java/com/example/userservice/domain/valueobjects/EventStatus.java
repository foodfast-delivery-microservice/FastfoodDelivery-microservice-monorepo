package com.example.userservice.domain.valueobjects;

/**
 * Value Object representing the status of an outbox event.
 * Used to track event processing lifecycle.
 */
public enum EventStatus {
    /** Event has been created but not yet processed */
    NEW,

    /** Event has been successfully processed and published */
    PROCESSED,

    /** Event processing failed */
    FAILED;

    /**
     * Check if this status represents a terminal state
     */
    public boolean isFinalState() {
        return this == PROCESSED || this == FAILED;
    }

    /**
     * Check if event can be reprocessed
     */
    public boolean canBeReprocessed() {
        return this == NEW || this == FAILED;
    }
}
