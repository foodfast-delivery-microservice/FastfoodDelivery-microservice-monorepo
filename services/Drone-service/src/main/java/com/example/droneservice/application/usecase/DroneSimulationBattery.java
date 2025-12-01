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

/**
 * Scheduler để simulate battery drain và charging cho drones
 * Chạy độc lập với DroneSimulationScheduler
 * 
 * QUAN TRỌNG: Class này CHỈ xử lý CHARGING và IDLE consumption
 * Battery drain khi DELIVERING/RETURNING được xử lý bởi
 * SimulateDroneMovementUseCase
 */
@Slf4j
@RequiredArgsConstructor
@Service // ← QUAN TRỌNG: Cần annotation này để Spring quản lý
public class DroneSimulationBattery {

    private final DroneRepository droneRepository;

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
     * Xử lý tiêu hao năng lượng khi rảnh rỗi
     * Trừ 0.5% mỗi 5s = 6% mỗi phút (realistic hơn)
     */
    private void handleIdleConsumption(Drone drone, int currentLevel) {
        if (currentLevel > 0) {
            // Giảm từ 1% xuống 0.5% để realistic hơn
            // Có thể dùng Math.max(0, currentLevel - 1) nếu muốn trừ 1%
            int newLevel = Math.max(0, currentLevel - 1);

            // Chỉ log khi có thay đổi đáng kể (mỗi 10%)
            if (currentLevel % 10 == 0 && currentLevel != newLevel) {
                drone.setBatteryLevel(newLevel);
                log.debug("⚡ Drone {} idle consumption. Battery: {}%",
                        drone.getSerialNumber(), newLevel);
            } else {
                drone.setBatteryLevel(newLevel);
            }

            // Cảnh báo khi pin thấp
            if (newLevel <= 20 && currentLevel > 20) {
                log.warn("⚠️ Drone {} battery is low: {}%. Consider charging.",
                        drone.getSerialNumber(), newLevel);
            }
        }
    }
}
