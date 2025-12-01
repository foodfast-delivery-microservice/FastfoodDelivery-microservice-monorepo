package com.example.droneservice.application.usecase;

import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.DroneMission;
import com.example.droneservice.domain.model.State;
import com.example.droneservice.domain.model.Status;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.domain.repository.DroneRepository;
import com.example.droneservice.infrastructure.util.GpsCoordinate;
import com.example.droneservice.infrastructure.util.HaversineDistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    /**
     * Simulate movement for a specific mission
     */
    @Transactional
    public void execute(Long missionId) {
        DroneMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionId));

        Drone drone = mission.getDrone();

        // Determine target based on mission status
        GpsCoordinate target = determineTarget(mission, drone);

        if (target == null) {
            log.warn("No target determined for mission {}", missionId);
            return;
        }

        // Calculate current position
        GpsCoordinate currentPos = new GpsCoordinate(
                drone.getCurrentLatitude(),
                drone.getCurrentLongitude());

        // Calculate distance to target
        double distanceToTarget = HaversineDistanceCalculator.calculate(
                currentPos.getLatitude(), currentPos.getLongitude(),
                target.getLatitude(), target.getLongitude());

        // Check if arrived at target
        if (distanceToTarget <= ARRIVAL_THRESHOLD_KM) {
            handleArrival(mission, drone);
            return;
        }

        // Calculate next position
        GpsCoordinate nextPos = HaversineDistanceCalculator.calculateNextPosition(
                currentPos, target, DRONE_SPEED_KMH, SIMULATION_INTERVAL_SECONDS);

        // Calculate distance traveled
        double distanceTraveled = HaversineDistanceCalculator.calculate(
                currentPos.getLatitude(), currentPos.getLongitude(),
                nextPos.getLatitude(), nextPos.getLongitude());

        // Update drone position
        drone.setCurrentLatitude(nextPos.getLatitude());
        drone.setCurrentLongitude(nextPos.getLongitude());

        // Update battery
        int batteryConsumed = (int) Math.ceil(distanceTraveled * BATTERY_CONSUMPTION_PER_KM);
        drone.setBatteryLevel(Math.max(0, drone.getBatteryLevel() - batteryConsumed));

        droneRepository.save(drone);

        log.debug("Drone {} moved to ({}, {}). Battery: {}%, Distance to target: {:.3f}km",
                drone.getSerialNumber(), nextPos.getLatitude(), nextPos.getLongitude(),
                drone.getBatteryLevel(), distanceToTarget);
    }

    /**
     * Determine the target coordinates based on mission status
     */
    private GpsCoordinate determineTarget(DroneMission mission, Drone drone) {
        // 1. Ưu tiên cao nhất: Đang quay về -> Mục tiêu là Base
        // (Bất kể Mission status là gì, nếu Drone state là RETURNING thì phải về)
        if (drone.getState() == State.RETURNING) {
            return new GpsCoordinate(drone.getBaseLatitude(), drone.getBaseLongitude());
        }

        // 2. Xử lý dựa trên Mission Status và Drone State
        return switch (mission.getStatus()) {

            // Trường hợp mới nhận nhiệm vụ: Chắc chắn phải đi lấy hàng
            case ASSIGNED ->
                    new GpsCoordinate(mission.getPickupLatitude(), mission.getPickupLongitude());

            case IN_PROGRESS -> {
                // Logic quan trọng: Phân định rõ đang đi Lấy hay đi Giao
                if (drone.getState() == State.DELIVERING) {
                    // Nếu trạng thái là ĐANG GIAO -> Bay đến nhà khách
                    yield new GpsCoordinate(mission.getDeliveryLatitude(), mission.getDeliveryLongitude());
                } else {
                    // Nếu trạng thái chưa phải DELIVERING (ví dụ vẫn là IDLE, LOADING...)
                    // -> Nghĩa là chưa lấy hàng xong -> Bay đến quán
                    yield new GpsCoordinate(mission.getPickupLatitude(), mission.getPickupLongitude());
                }
            }

            // Các trạng thái kết thúc -> Không cần di chuyển (hoặc đã xử lý ở RETURNING trên cùng)
            case COMPLETED, CANCELLED -> null;
        };
    }

    /**
     * Handle arrival at target location
     */
    private void handleArrival(DroneMission mission, Drone drone) {
        // get current location of drone
        GpsCoordinate currentPos = new GpsCoordinate(
                drone.getCurrentLatitude(),
                drone.getCurrentLongitude());

        // calculate distance to pickup and delivery
        // Check which location we arrived at
        double distanceToPickup = HaversineDistanceCalculator.calculate(
                currentPos.getLatitude(), currentPos.getLongitude(),
                mission.getPickupLatitude(), mission.getPickupLongitude());

        double distanceToDelivery = HaversineDistanceCalculator.calculate(
                currentPos.getLatitude(), currentPos.getLongitude(),
                mission.getDeliveryLatitude(), mission.getDeliveryLongitude());

        double distanceToBase = HaversineDistanceCalculator.calculate(
                currentPos.getLatitude(), currentPos.getLongitude(),
                drone.getBaseLatitude(), drone.getBaseLongitude());


        // todo check where the drone is

        // case 1: Drone is at pickup location
        if (distanceToPickup <= ARRIVAL_THRESHOLD_KM) {
            log.info("Drone {} arrived at PICKUP location for order {}",
                    drone.getSerialNumber(), mission.getOrderId());
            mission.setStatus(Status.IN_PROGRESS);
            missionRepository.save(mission);

        } 
        
        // case 2: Drone is at delivery location
        else if (distanceToDelivery <= ARRIVAL_THRESHOLD_KM) {
            log.info("Drone {} DELIVERED order {}",
                    drone.getSerialNumber(), mission.getOrderId());

            // Start returning to base
            drone.setState(State.RETURNING);
            droneRepository.save(drone);

        }
        
        // case 3: Drone is at base location
        else if (distanceToBase <= ARRIVAL_THRESHOLD_KM) {
            log.info("Drone {} returned to BASE. Mission {} completed",
                    drone.getSerialNumber(), mission.getId());

            // Mission complete
            mission.setStatus(Status.COMPLETED);
            mission.setCompletedAt(LocalDateTime.now());
            missionRepository.save(mission);

            // Drone back to idle or charging
            if (drone.getBatteryLevel() < 50) {
                drone.setState(State.CHARGING);
                log.info("Drone {} started CHARGING (Battery: {}%)",
                        drone.getSerialNumber(), drone.getBatteryLevel());
            } else {
                drone.setState(State.IDLE);
                log.info("Drone {} is now IDLE (Battery: {}%)",
                        drone.getSerialNumber(), drone.getBatteryLevel());
            }
            droneRepository.save(drone);
        }
    }
}
