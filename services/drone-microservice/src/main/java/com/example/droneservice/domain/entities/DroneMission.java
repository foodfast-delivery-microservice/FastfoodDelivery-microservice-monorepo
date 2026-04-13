package com.example.droneservice.domain.entities;

import com.example.droneservice.domain.valueobjects.Coordinates;
import com.example.droneservice.domain.valueobjects.Status;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Pure domain entity representing a Drone Mission.
 * Contains business logic for mission management, independent of persistence
 * framework.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DroneMission {
    private Long id;
    private Drone drone;
    private Long orderId;
    private Coordinates pickupLocation;
    private Coordinates deliveryLocation;
    private Status status;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    /**
     * Calculate total distance for the mission: Base → Pickup → Delivery → Base
     */
    public double calculateTotalDistance(Coordinates baseLocation) {
        double baseToPickup = baseLocation.distanceTo(pickupLocation);
        double pickupToDelivery = pickupLocation.distanceTo(deliveryLocation);
        double deliveryToBase = deliveryLocation.distanceTo(baseLocation);

        return baseToPickup + pickupToDelivery + deliveryToBase;
    }

    /**
     * Start the mission - transition from ASSIGNED to IN_PROGRESS
     */
    public void start() {
        if (!status.canTransitionTo(Status.IN_PROGRESS)) {
            throw new IllegalStateException(
                    "Cannot start mission. Current status: " + status);
        }
        this.status = Status.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * Complete the mission successfully
     */
    public void complete() {
        if (!status.canTransitionTo(Status.COMPLETED)) {
            throw new IllegalStateException(
                    "Cannot complete mission. Current status: " + status);
        }
        this.status = Status.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Cancel the mission
     */
    public void cancel() {
        if (!status.canTransitionTo(Status.CANCELLED)) {
            throw new IllegalStateException(
                    "Cannot cancel mission. Current status: " + status);
        }
        this.status = Status.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Check if mission is in progress
     */
    public boolean isInProgress() {
        return status == Status.IN_PROGRESS;
    }

    /**
     * Check if mission is completed
     */
    public boolean isCompleted() {
        return status.isFinal();
    }

}
