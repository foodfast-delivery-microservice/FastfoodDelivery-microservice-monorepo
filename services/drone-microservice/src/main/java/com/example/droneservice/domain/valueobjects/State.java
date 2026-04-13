package com.example.droneservice.domain.valueobjects;

/**
 * Represents the current state of a drone.
 * Enumerates all possible drone operational states.
 */
public enum State {
    IDLE,
    DELIVERING,
    RETURNING,
    CHARGING,
    MAINTENANCE;

    /**
     * Check if the drone is in an idle state and can accept new missions
     */
    public boolean isIdle() {
        return this == IDLE;
    }

    /**
     * Check if the drone is currently active (not idle, not charging, not in
     * maintenance)
     */
    public boolean isActive() {
        return this == DELIVERING || this == RETURNING;
    }

    /**
     * Validate if transition to a new state is allowed
     */
    public boolean canTransitionTo(State newState) {
        if (newState == null) {
            return false;
        }

        // Define valid state transitions
        return switch (this) {
            case IDLE -> newState == DELIVERING || newState == CHARGING || newState == MAINTENANCE;
            case DELIVERING -> newState == RETURNING || newState == IDLE;
            case RETURNING -> newState == IDLE || newState == CHARGING;
            case CHARGING -> newState == IDLE || newState == MAINTENANCE;
            case MAINTENANCE -> newState == IDLE || newState == CHARGING;
        };
    }
}
