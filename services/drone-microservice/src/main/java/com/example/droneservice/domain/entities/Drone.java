package com.example.droneservice.domain.entities;

import com.example.droneservice.domain.valueobjects.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure domain entity representing a Drone.
 * Contains business logic and domain rules, independent of persistence
 * framework.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Drone {
    private Long id;
    private SerialNumber serialNumber;
    private String model;
    private BatteryLevel batteryLevel;
    private State state;
    private Coordinates currentLocation;
    private Coordinates baseLocation;
    private WeightCapacity weightCapacity;
    private List<DroneMission> missions;

    /**
     * Business rule: Check if drone can accept a new mission
     * Validates battery sufficiency and state availability
     */
    public boolean canAcceptMission(double weightKg, double totalDistanceKm) {
        if (!state.isIdle()) {
            return false;
        }

        if (!weightCapacity.canCarry(weightKg)) {
            return false;
        }

        // Check battery with 10% reserve
        return batteryLevel.canSupport(totalDistanceKm, 10.0);
    }

    /**
     * Business rule: Assign a mission to drone
     * Validates and performs state transition
     */
    public void assignMission(DroneMission mission) {
        double totalDistance = mission.calculateTotalDistance(this.baseLocation);

        if (!canAcceptMission(0.0, totalDistance)) { // Weight check done elsewhere
            throw new IllegalStateException(
                    "Drone " + serialNumber.getValue() + " cannot accept mission. " +
                            "State: " + state + ", Battery: " + batteryLevel);
        }

        if (!state.canTransitionTo(State.DELIVERING)) {
            throw new IllegalStateException(
                    "Cannot transition from " + state + " to DELIVERING");
        }

        this.state = State.DELIVERING;

        if (this.missions == null) {
            this.missions = new ArrayList<>();
        }
        this.missions.add(mission);
    }

    /**
     * Complete a mission and return to base
     */
    public void completeMission(double distanceTraveled) {
        if (!state.isActive()) {
            throw new IllegalStateException("Drone is not in active state");
        }

        // Consume battery
        this.batteryLevel = batteryLevel.afterDistance(distanceTraveled);

        // Transition to RETURNING or IDLE
        if (state == State.DELIVERING) {
            this.state = State.RETURNING;
        } else if (state == State.RETURNING) {
            this.state = State.IDLE;
            this.currentLocation = this.baseLocation;
        }
    }

    /**
     * Update battery level (e.g., during charging)
     */
    public void updateBattery(int newLevel) {
        this.batteryLevel = new BatteryLevel(newLevel);
    }

    /**
     * Update current location
     */
    public void updateLocation(Coordinates newLocation) {
        this.currentLocation = newLocation;
    }

    /**
     * Change drone state
     */
    public void changeState(State newState) {
        if (!this.state.canTransitionTo(newState)) {
            throw new IllegalStateException(
                    "Invalid state transition from " + this.state + " to " + newState);
        }
        this.state = newState;
    }
}
