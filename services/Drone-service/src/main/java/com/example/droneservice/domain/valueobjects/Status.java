package com.example.droneservice.domain.valueobjects;

/**
 * Represents the status of a drone mission.
 * Tracks the lifecycle of a delivery mission.
 */
public enum Status {
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    /**
     * Check if the mission is in a final state
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * Check if the mission is active
     */
    public boolean isActive() {
        return this == ASSIGNED || this == IN_PROGRESS;
    }

    /**
     * Validate if transition to a new status is allowed
     */
    public boolean canTransitionTo(Status newStatus) {
        if (newStatus == null) {
            return false;
        }

        // Cannot transition from final states
        if (this.isFinal()) {
            return false;
        }

        return switch (this) {
            case ASSIGNED -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == COMPLETED || newStatus == CANCELLED;
            default -> false;
        };
    }
}
