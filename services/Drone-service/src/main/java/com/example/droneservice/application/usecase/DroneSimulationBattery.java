package com.example.droneservice.application.usecase;

import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.State;
import com.example.droneservice.domain.repository.DroneRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduler để simulate battery drain và charging cho drones
 * Chạy độc lập với DroneSimulationScheduler
 * <p>
 * QUAN TRỌNG: Class này CHỈ xử lý CHARGING và IDLE consumption
 * Battery drain khi DELIVERING/RETURNING được xử lý bởi
 * SimulateDroneMovementUseCase
 */
@Slf4j
@RequiredArgsConstructor
@Service // ← QUAN TRỌNG: Cần annotation này để Spring quản lý
public class DroneSimulationBattery {

    private final DroneRepository droneRepository;

    // Đếm số tick IDLE cho từng drone để trừ pin rất chậm
    // Key: droneId, Value: số lần tick (mỗi tick = 5s)
    private final ConcurrentHashMap<Long, Integer> idleTickCounter = new ConcurrentHashMap<>();

    // Chạy mỗi 5 giây
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void simulateBatteryDrainAndCharge() {
        // findAll rất lãng phí vì ví dụ có 1000 con drone thì có 100 con cần sạc nhưng nó vẫn quét qua 900 con kia
        //List<Drone> drones = droneRepository.findAll();
        List<Drone> drones = droneRepository.findAllByStateIn(List.of(State.IDLE, State.CHARGING));
        for (Drone drone : drones) {
            updateDroneBattery(drone);
        }

        // Save all changes into database
        droneRepository.saveAll(drones);
    }

    private void updateDroneBattery(Drone drone) {
        int currentLevel = drone.getBatteryLevel();
        State currentState = drone.getState();

        switch (currentState) {
            case CHARGING:
                handleCharging(drone, currentLevel);
                break;

            case IDLE:
                handleIdleConsumption(drone, currentLevel);
                break;

            case MAINTENANCE:
                // Không làm gì khi đang bảo trì
                break;

            // DELIVERING và RETURNING được xử lý bởi SimulateDroneMovementUseCase
            // Không xử lý ở đây để tránh trừ battery 2 lần
            case DELIVERING:
            case RETURNING:
                // Do nothing - handled by SimulateDroneMovementUseCase
                break;
        }
    }

    /**
     * Xử lý logic sạc pin
     * Sạc 5% mỗi 5 giây = 60% mỗi phút
     */
    private void handleCharging(Drone drone, int currentLevel) {
        if (currentLevel < 100) {
            int newLevel = Math.min(100, currentLevel + 5); // +5% mỗi 5s
            drone.setBatteryLevel(newLevel);
            log.info("🔌 Drone {} is charging. Battery: {}% → {}%",
                    drone.getSerialNumber(), currentLevel, newLevel);
        }

        // Tự động chuyển sang IDLE khi sạc đầy
        if (drone.getBatteryLevel() == 100) {
            drone.setState(State.IDLE);
            log.info("✅ Drone {} fully charged. Switched to IDLE.",
                    drone.getSerialNumber());
        }
    }

    /**
     * Xử lý tiêu hao năng lượng khi rảnh rỗi (IDLE)
     * Mục tiêu: pin tụt rất chậm, chỉ giảm ~1% mỗi 5 phút
     * - Scheduler tick mỗi 5s → 60 tick = 5 phút
     * - Sau 60 tick mới trừ 1%
     */
    private void handleIdleConsumption(Drone drone, int currentLevel) {
        if (currentLevel <= 0) {
            // Clear counter when hết pin
            idleTickCounter.remove(drone.getId());
            return;
        }

        Long droneId = drone.getId();
        int ticks = idleTickCounter.getOrDefault(droneId, 0) + 1;

        // Mỗi 60 tick (≈ 5 phút) mới trừ 1% pin
        if (ticks >= 60) {
            int newLevel = Math.max(0, currentLevel - 1);
            drone.setBatteryLevel(newLevel);
            idleTickCounter.put(droneId, 0); // reset counter

            log.debug("⚡ Drone {} idle consumption. Battery: {}% → {}% (every ~5 minutes)",
                    drone.getSerialNumber(), currentLevel, newLevel);

            // Cảnh báo khi pin thấp
            if (newLevel <= 20 && currentLevel > 20) {
                log.warn("⚠️ Drone {} battery is low: {}%. Consider charging.",
                        drone.getSerialNumber(), newLevel);
            }
        } else {
            // Chưa tới ngưỡng trừ pin, chỉ tăng bộ đếm
            idleTickCounter.put(droneId, ticks);
            log.trace("⚡ Drone {} idle tick {}/60 (no battery change). Current: {}%",
                    drone.getSerialNumber(), ticks, currentLevel);
        }
    }
}
