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

        // Get all idle drones (có thể mở rộng để bao gồm CHARGING nếu đủ pin)
        var idleDrones = droneRepository.findByState(State.IDLE);

        if (idleDrones.isEmpty()) {
            log.warn("⚠️ No idle drones available");
            return Optional.empty();
        }

        log.info("📊 Checking {} idle drones for battery sufficiency", idleDrones.size());

        // Find drone with sufficient battery for complete route
        for (Drone drone : idleDrones) {
            // Validate base coordinates
            if (drone.getBaseLatitude() == null || drone.getBaseLongitude() == null) {
                log.warn("⚠️ Drone {} has no base coordinates, skipping", drone.getSerialNumber());
                continue;
            }

            // Calculate total distance: Base → Pickup → Delivery → Base
            double requiredBattery = calculateRequiredBattery(
                    drone.getBaseLatitude(), drone.getBaseLongitude(),
                    pickupLat, pickupLon,
                    deliveryLat, deliveryLon);

            // Total battery needed = required + reserve
            double totalBatteryNeeded = requiredBattery + MINIMUM_BATTERY_RESERVE;
            int currentBattery = drone.getBatteryLevel();

            if (currentBattery >= totalBatteryNeeded) {
                log.info("✅ Found available drone: {} (Battery: {}%, Required: {:.1f}%, Reserve: {}%, Total needed: {:.1f}%)",
                        drone.getSerialNumber(), currentBattery, requiredBattery, 
                        MINIMUM_BATTERY_RESERVE, totalBatteryNeeded);
                return Optional.of(drone);
            } else {
                log.debug("❌ Drone {} has insufficient battery: {}% (Required: {:.1f}% + Reserve: {}% = {:.1f}%)",
                        drone.getSerialNumber(), currentBattery, requiredBattery, 
                        MINIMUM_BATTERY_RESERVE, totalBatteryNeeded);
            }
        }

        log.warn("❌ No drones with sufficient battery found for this route");
        return Optional.empty();
    }

    /**
     * Calculate total battery required for the complete mission route:
     * Base → Pickup (nhà hàng) → Delivery (khách hàng) → Base
     * 
     * Tổng quãng đường = Base→Pickup + Pickup→Delivery + Delivery→Base
     * Pin cần thiết = Tổng quãng đường × 2% mỗi km
     */
    private double calculateRequiredBattery(Double baseLat, Double baseLon,
                                            Double pickupLat, Double pickupLon,
                                            Double deliveryLat, Double deliveryLon) {

        // 1. Distance from base to pickup (nhà hàng)
        double baseToPickup = HaversineDistanceCalculator.calculate(
                baseLat, baseLon, pickupLat, pickupLon);

        // 2. Distance from pickup to delivery (khách hàng)
        double pickupToDelivery = HaversineDistanceCalculator.calculate(
                pickupLat, pickupLon, deliveryLat, deliveryLon);

        // 3. Distance from delivery back to base
        double deliveryToBase = HaversineDistanceCalculator.calculate(
                deliveryLat, deliveryLon, baseLat, baseLon);

        // Tổng quãng đường = Base→Pickup + Pickup→Delivery + Delivery→Base
        double totalDistance = baseToPickup + pickupToDelivery + deliveryToBase;
        
        // Pin cần thiết = Tổng quãng đường × 2% mỗi km
        double requiredBattery = totalDistance * BATTERY_CONSUMPTION_PER_KM;

        log.info(
                "📏 Route calculation - Base→Pickup: {:.2f}km, Pickup→Delivery: {:.2f}km, Delivery→Base: {:.2f}km, Total: {:.2f}km, Required Battery: {:.1f}%",
                baseToPickup, pickupToDelivery, deliveryToBase, totalDistance, requiredBattery);

        return requiredBattery;
    }
}
