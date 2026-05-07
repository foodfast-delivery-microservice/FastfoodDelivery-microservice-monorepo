package com.example.notificationservice.domain.valueobjects;

/**
 * Enum representing email notification status.
 */
public enum EmailStatus {
    PENDING,      // Email queued, not yet sent
    SENT,         // Email sent successfully
    FAILED,       // Email sending failed
    RETRYING,     // Email is being retried
    SKIPPED       // Email skipped due to undeliverable recipient
}
