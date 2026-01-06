package com.example.droneservice.application.usecase;

import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.entities.DroneMission;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.domain.repository.DroneRepository;
import com.example.droneservice.domain.valueobjects.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core simulation use case - simulates drone movement for a single mission.
 * This is called by the scheduler every 2 seconds for each active mission.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulateDroneMovementUseCase {

    private final DroneRepository droneRepository;
    private final DroneMissionRepository missionRepository;

    private static final double DRONE_SPEED_KMH = 40.0; // 40 km/h
    private static final int SIMULATION_INTERVAL_SECONDS = 2; // Called every 2 seconds
    private static final double BATTERY_CONSUMPTION_PER_KM = 2.0; // 2% per km
    private static final double ARRIVAL_THRESHOLD_KM = 0.05; // 50 meters = arrived

    // Accumulate fractional battery consumption per drone to avoid rounding errors
    // Key: droneId, Value: accumulated battery consumption (percentage)
    private final ConcurrentHashMap<Long, Double> accumulatedBatteryConsumption = new ConcurrentHashMap<>();

    /**
     * Simulate movement for a specific mission
     */
    @Transactional
    public void execute(Long missionId) {
        DroneMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionId));

        Drone drone = mission.getDrone();

        // Check if drone is in a state that should consume battery
        // Only DELIVERING and RETURNING drones should consume battery during movement
        State droneState = drone.getState();
        if (droneState != State.DELIVERING && droneState != State.RETURNING) {
            // Clear any accumulated battery consumption for non-active drones
            accumulatedBatteryConsumption.remove(drone.getId());
            log.debug("⏸️ Drone {} is in state {} - skipping battery consumption",
                    drone.getSerialNumber().getValue(), droneState);
            return;
        }

        // CRITICAL: Check if drone has run out of battery
        if (drone.getBatteryLevel().getValue() <= 5) {
            handleBatteryDepleted(mission, drone);
            return;
        }

        // Determine target based on mission status
        Coordinates target = determineTarget(mission, drone);

        if (target == null) {
            log.warn("No target determined for mission {} (drone state: {})", missionId, droneState);
            // Clear accumulated battery when no target (mission completed/cancelled)
            accumulatedBatteryConsumption.remove(drone.getId());
            return;
        }

        // Get current position from drone
        Coordinates currentPos = drone.getCurrentLocation();

        // Calculate distance to target using Coordinates
        double distanceToTarget = currentPos.distanceTo(target);

        // Check if arrived at target
        if (distanceToTarget <= ARRIVAL_THRESHOLD_KM) {
            handleArrival(mission, drone);
            return;
        }

        // Calculate next position
        Coordinates nextPos = currentPos.moveTowards(target, DRONE_SPEED_KMH, SIMULATION_INTERVAL_SECONDS);

        // Calculate distance traveled
        double distanceTraveled = currentPos.distanceTo(nextPos);

        // Update drone position using Coordinates
        drone.setCurrentLocation(nextPos);

        // Update battery with accumulation to avoid over-consumption from Math.ceil()
        // Problem: Math.ceil(0.044%) = 1% means 1% consumed every 2 seconds =
        // 30%/minute (too fast!)
        // Solution: Accumulate fractional consumption and only deduct when >= 1%
        // IMPORTANT: Only consume battery when drone is DELIVERING or RETURNING
        // IDLE drones should NOT consume battery here (handled by
        // DroneSimulationBattery)
        // droneState already checked above, so we know it's DELIVERING or RETURNING
        // here
        if (droneState == State.DELIVERING || droneState == State.RETURNING) {
            double batteryConsumed = distanceTraveled * BATTERY_CONSUMPTION_PER_KM;
            double accumulated = accumulatedBatteryConsumption.getOrDefault(drone.getId(), 0.0);
            accumulated += batteryConsumed;

            // Only deduct battery when accumulated consumption >= 1%
            if (accumulated >= 1.0) {
                int batteryToDeduct = (int) Math.floor(accumulated);
                int currentBattery = drone.getBatteryLevel().getValue();
                int newBatteryLevel = Math.max(0, currentBattery - batteryToDeduct);
                drone.setBatteryLevel(new BatteryLevel(newBatteryLevel));
                accumulated -= batteryToDeduct; // Keep the remainder
                log.debug("🔋 Drone {} battery deducted: {}% (accumulated: {:.3f}%)",
                        drone.getSerialNumber().getValue(), batteryToDeduct, accumulated);

                // Check if battery depleted after deduction
                if (newBatteryLevel <= 0) {
                    handleBatteryDepleted(mission, drone);
                    return;
                }
            }

            // Store accumulated value for next iteration
            accumulatedBatteryConsumption.put(drone.getId(), accumulated);
        } else {
            // Clear accumulated value if drone is not DELIVERING or RETURNING
            // This prevents battery consumption for IDLE drones
            accumulatedBatteryConsumption.remove(drone.getId());
            log.debug("🧹 Cleared accumulated battery for drone {} (state: {})",
                    drone.getSerialNumber().getValue(), droneState);
        }

        droneRepository.save(drone);

        log.info("🚁 Drone {} moved to ({}, {}). Battery: {}%, Distance to target: {:.3f}km",
                drone.getSerialNumber().getValue(), nextPos.getLatitude(), nextPos.getLongitude(),
                drone.getBatteryLevel().getValue(), distanceToTarget);
    }

    /**
     * Determine the target coordinates based on mission status
     */
    private Coordinates determineTarget(DroneMission mission, Drone drone) {
        // 1. Ưu tiên cao nhất: Đang quay về -> Mục tiêu là Base
        // (Bất kể Mission status là gì, nếu Drone state là RETURNING thì phải về)
        if (drone.getState() == State.RETURNING) {
            return drone.getBaseLocation();
        }

        // 2. Xử lý dựa trên Mission Status và Drone State
        return switch (mission.getStatus()) {

            // Trường hợp mới nhận nhiệm vụ: Chắc chắn phải đi lấy hàng
            case ASSIGNED -> mission.getPickupLocation();

            case IN_PROGRESS -> {
                // Logic quan trọng: Phân định rõ đang đi Lấy hay đi Giao
                if (drone.getState() == State.DELIVERING) {
                    // Nếu trạng thái là ĐANG GIAO -> Bay đến nhà khách
                    yield mission.getDeliveryLocation();
                } else {
                    // Nếu trạng thái chưa phải DELIVERING (ví dụ vẫn là IDLE, LOADING...)
                    // -> Nghĩa là chưa lấy hàng xong -> Bay đến quán
                    yield mission.getPickupLocation();
                }
            }

            // Các trạng thái kết thúc -> Không cần di chuyển (hoặc đã xử lý ở RETURNING
            // trên cùng)
            case COMPLETED, CANCELLED -> null;
        };
    }

    /**
     * Handle arrival at target location
     */
    private void handleArrival(DroneMission mission, Drone drone) {
        // Get current location of drone
        Coordinates currentPos = drone.getCurrentLocation();

        // Calculate distance to pickup, delivery, and base using Coordinates
        double distanceToPickup = currentPos.distanceTo(mission.getPickupLocation());
        double distanceToDelivery = currentPos.distanceTo(mission.getDeliveryLocation());
        double distanceToBase = currentPos.distanceTo(drone.getBaseLocation());

        // Check where the drone is

        // case 1: Drone is at pickup location (nhà hàng)
        if (distanceToPickup <= ARRIVAL_THRESHOLD_KM) {
            log.info("✅ Drone {} arrived at PICKUP location (nhà hàng) for order {}",
                    drone.getSerialNumber().getValue(), mission.getOrderId());
            // Chuyển mission status thành IN_PROGRESS để bắt đầu giao hàng
            mission.setStatus(Status.IN_PROGRESS);
            // Đảm bảo drone state là DELIVERING để đi đến delivery location
            if (drone.getState() != State.DELIVERING) {
                drone.setState(State.DELIVERING);
            }
            missionRepository.save(mission);
            droneRepository.save(drone);
            log.info("📦 Mission {} status changed to IN_PROGRESS - Drone will now go to delivery location",
                    mission.getId());
        }

        // case 2: Drone is at delivery location (đã giao hàng)
        else if (distanceToDelivery <= ARRIVAL_THRESHOLD_KM) {
            log.info("✅ Drone {} DELIVERED order {} - Gửi event để order status = 'delivered'",
                    drone.getSerialNumber().getValue(), mission.getOrderId());

            // Start returning to base
            drone.setState(State.RETURNING);
            // Keep accumulated battery consumption for RETURNING state
            droneRepository.save(drone);

            // Note: Event sẽ được publish bởi DroneSimulationScheduler
            // khi detect drone state = DELIVERING hoặc vừa chuyển sang RETURNING
        }

        // case 3: Drone is at base location
        else if (distanceToBase <= ARRIVAL_THRESHOLD_KM) {
            log.info("Drone {} returned to BASE. Mission {} completed",
                    drone.getSerialNumber().getValue(), mission.getId());

            // Mission complete
            mission.setStatus(Status.COMPLETED);
            mission.setCompletedAt(LocalDateTime.now());
            missionRepository.save(mission);

            // Clear accumulated battery consumption when mission completes
            accumulatedBatteryConsumption.remove(drone.getId());

            // Drone back to idle or charging
            if (drone.getBatteryLevel().getValue() < 50) {
                drone.setState(State.CHARGING);
                log.info("Drone {} started CHARGING (Battery: {}%)",
                        drone.getSerialNumber().getValue(), drone.getBatteryLevel().getValue());
            } else {
                drone.setState(State.IDLE);
                log.info("Drone {} is now IDLE (Battery: {}%)",
                        drone.getSerialNumber().getValue(), drone.getBatteryLevel().getValue());
            }
            droneRepository.save(drone);
        }
    }

    /**
     * Handle battery depletion - drone hết pin giữa chừng
     * Khi drone hết pin, nó sẽ:
     * 1. Dừng lại tại vị trí hiện tại
     * 2. Chuyển state sang MAINTENANCE
     * 3. Mission có thể bị CANCELLED hoặc cần rescue
     */
    private void handleBatteryDepleted(DroneMission mission, Drone drone) {
        // Save current state before changing it
        State previousState = drone.getState();

        log.error("🔴 CRITICAL: Drone {} has run out of battery during mission {} (Order {})!",
                drone.getSerialNumber().getValue(), mission.getId(), mission.getOrderId());
        log.error("📍 Drone location: {}, Previous state: {}, Battery: {}%",
                drone.getCurrentLocation(), previousState, drone.getBatteryLevel().getValue());

        // Set battery to 0 to prevent negative values
        drone.setBatteryLevel(new BatteryLevel(0));

        // Change drone state to MAINTENANCE (cần sửa chữa/nạp pin)
        drone.setState(State.MAINTENANCE);
        droneRepository.save(drone);

        // Mission status depends on where drone ran out:
        // - If returning to base: Mission can be marked as completed (delivery was
        // successful)
        // - If going to pickup/delivery: Mission should be cancelled
        if (previousState == State.RETURNING) {
            // Drone was returning to base after delivery - delivery was successful
            log.warn("⚠️ Drone ran out of battery while returning to base. Delivery was successful.");
            mission.setStatus(Status.COMPLETED);
            mission.setCompletedAt(LocalDateTime.now());
        } else {
            // Drone ran out before completing delivery - mission failed
            log.error("❌ Mission {} CANCELLED due to battery depletion", mission.getId());
            mission.setStatus(Status.CANCELLED);
        }

        missionRepository.save(mission);

        // Clear accumulated battery consumption
        accumulatedBatteryConsumption.remove(drone.getId());

        log.error("🚨 ACTION REQUIRED: Drone {} needs immediate attention! Location: {}",
                drone.getSerialNumber().getValue(), drone.getCurrentLocation());
    }
}
