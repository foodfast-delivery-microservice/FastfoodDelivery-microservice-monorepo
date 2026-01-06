package com.example.droneservice.application.usecase;

import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.valueobjects.Coordinates;
import com.example.droneservice.domain.valueobjects.State;
import com.example.droneservice.domain.repository.DroneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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

        private static final int MINIMUM_BATTERY_RESERVE = 10; // Keep 10% reserve

        /**
         * Find an available drone for the given route
         * 
         * Validation: Chỉ gán drone nếu đủ pin để hoàn thành toàn bộ lộ trình:
         * Base → Pickup → Delivery → Base
         *
         * @param pickupLat   Pickup location latitude (nhà hàng)
         * @param pickupLon   Pickup location longitude (nhà hàng)
         * @param deliveryLat Delivery location latitude (khách hàng)
         * @param deliveryLon Delivery location longitude (khách hàng)
         * @return Optional containing available drone, or empty if none available
         */
        public Optional<Drone> execute(Double pickupLat, Double pickupLon,
                        Double deliveryLat, Double deliveryLon) {

                log.info("🔍 Searching for available drone for route: pickup({}, {}) -> delivery({}, {})",
                                pickupLat, pickupLon, deliveryLat, deliveryLon);

                // Get all idle drones
                List<Drone> idleDrones = droneRepository.findByState(State.IDLE);

                if (idleDrones.isEmpty()) {
                        log.warn("⚠️ No idle drones available");
                        return Optional.empty();
                }

                log.info("📊 Checking {} idle drones for battery sufficiency", idleDrones.size());

                // Create coordinates for pickup and delivery
                Coordinates pickupLocation = new Coordinates(pickupLat, pickupLon);
                Coordinates deliveryLocation = new Coordinates(deliveryLat, deliveryLon);

                // Find drone with sufficient battery for complete route
                for (Drone drone : idleDrones) {
                        // Validate base coordinates
                        if (drone.getBaseLocation() == null) {
                                log.warn("⚠️ Drone {} has no base coordinates, skipping",
                                                drone.getSerialNumber().getValue());
                                continue;
                        }

                        // Calculate total distance using Coordinates value object
                        double totalDistance = calculateTotalDistance(
                                        drone.getBaseLocation(),
                                        pickupLocation,
                                        deliveryLocation);

                        // Check battery using BatteryLevel value object method
                        if (drone.getBatteryLevel().canSupport(totalDistance, MINIMUM_BATTERY_RESERVE)) {
                                double requiredBattery = totalDistance * 2.0; // Battery consumption per km
                                log.info("✅ Found available drone: {} (Battery: {}%, Required: {:.1f}%, Reserve: {}%)",
                                                drone.getSerialNumber().getValue(),
                                                drone.getBatteryLevel().getValue(),
                                                requiredBattery,
                                                MINIMUM_BATTERY_RESERVE);
                                return Optional.of(drone);
                        } else {
                                log.debug("❌ Drone {} has insufficient battery",
                                                drone.getSerialNumber().getValue());
                        }
                }

                log.warn("❌ No drones with sufficient battery found for this route");
                return Optional.empty();
        }

        /**
         * Calculate total distance for the complete mission route:
         * Base → Pickup (nhà hàng) → Delivery (khách hàng) → Base
         */
        private double calculateTotalDistance(Coordinates baseLocation,
                        Coordinates pickupLocation,
                        Coordinates deliveryLocation) {

                // 1. Distance from base to pickup
                double baseToPickup = baseLocation.distanceTo(pickupLocation);

                // 2. Distance from pickup to delivery
                double pickupToDelivery = pickupLocation.distanceTo(deliveryLocation);

                // 3. Distance from delivery back to base
                double deliveryToBase = deliveryLocation.distanceTo(baseLocation);

                // Total distance
                double totalDistance = baseToPickup + pickupToDelivery + deliveryToBase;

                log.info(
                                "📏 Route calculation - Base→Pickup: {:.2f}km, Pickup→Delivery: {:.2f}km, Delivery→Base: {:.2f}km, Total: {:.2f}km",
                                baseToPickup, pickupToDelivery, deliveryToBase, totalDistance);

                return totalDistance;
        }
}
