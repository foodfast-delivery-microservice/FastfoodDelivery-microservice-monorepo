package com.example.droneservice.application.usecase;

import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.State;
import com.example.droneservice.domain.repository.DroneRepository;
import com.example.droneservice.infrastructure.util.HaversineDistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case to find an available drone for a delivery mission.
 * A drone is considered available if:
 * 1. It's in IDLE state
 * 2. It has enough battery to complete the round trip (pickup + delivery +
 * return to base)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetAvailableDroneUseCase {

    private final DroneRepository droneRepository;

    private static final double BATTERY_CONSUMPTION_PER_KM = 2.0; // 2% battery per km
    private static final int MINIMUM_BATTERY_RESERVE = 10; // Keep 10% reserve

    /**
     * Find an available drone for the given route
     * 
     * @param pickupLat   Pickup location latitude
     * @param pickupLon   Pickup location longitude
     * @param deliveryLat Delivery location latitude
     * @param deliveryLon Delivery location longitude
     * @return Optional containing available drone, or empty if none available
     */
    public Optional<Drone> execute(Double pickupLat, Double pickupLon,
            Double deliveryLat, Double deliveryLon) {

        log.info("Searching for available drone for route: pickup({}, {}) -> delivery({}, {})",
                pickupLat, pickupLon, deliveryLat, deliveryLon);

        // Get all idle drones
        var idleDrones = droneRepository.findByState(State.IDLE);

        if (idleDrones.isEmpty()) {
            log.warn("No idle drones available");
            return Optional.empty();
        }

        // Find drone with sufficient battery
        for (Drone drone : idleDrones) {
            double requiredBattery = calculateRequiredBattery(
                    drone.getBaseLatitude(), drone.getBaseLongitude(),
                    pickupLat, pickupLon,
                    deliveryLat, deliveryLon);

            if (drone.getBatteryLevel() >= requiredBattery + MINIMUM_BATTERY_RESERVE) {
                log.info("Found available drone: {} (Battery: {}%, Required: {}%)",
                        drone.getSerialNumber(), drone.getBatteryLevel(), requiredBattery);
                return Optional.of(drone);
            } else {
                log.debug("Drone {} has insufficient battery: {}% (Required: {}%)",
                        drone.getSerialNumber(), drone.getBatteryLevel(), requiredBattery);
            }
        }

        log.warn("No drones with sufficient battery found");
        return Optional.empty();
    }

    /**
     * Calculate total battery required for the mission:
     * Base -> Pickup -> Delivery -> Base
     */
    private double calculateRequiredBattery(Double baseLat, Double baseLon,
            Double pickupLat, Double pickupLon,
            Double deliveryLat, Double deliveryLon) {

        // Distance from base to pickup
        double baseToPickup = HaversineDistanceCalculator.calculate(
                baseLat, baseLon, pickupLat, pickupLon);

        // Distance from pickup to delivery
        double pickupToDelivery = HaversineDistanceCalculator.calculate(
                pickupLat, pickupLon, deliveryLat, deliveryLon);

        // Distance from delivery back to base
        double deliveryToBase = HaversineDistanceCalculator.calculate(
                deliveryLat, deliveryLon, baseLat, baseLon);

        double totalDistance = baseToPickup + pickupToDelivery + deliveryToBase;
        double requiredBattery = totalDistance * BATTERY_CONSUMPTION_PER_KM;

        log.debug(
                "Route distances - Base->Pickup: {:.2f}km, Pickup->Delivery: {:.2f}km, Delivery->Base: {:.2f}km, Total: {:.2f}km, Battery: {:.1f}%",
                baseToPickup, pickupToDelivery, deliveryToBase, totalDistance, requiredBattery);

        return requiredBattery;
    }
}
