package com.example.paymentservice.domain.valueobjects;

/**
 * Value Object representing the status of an outbox event.
 */
public enum EventStatus {
    NEW,
    PROCESSED,
    FAILED
}
