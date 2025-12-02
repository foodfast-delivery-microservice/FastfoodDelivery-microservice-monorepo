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
    
    // Accumulate fractional battery consumption for IDLE drones
    // Key: droneId, Value: accumulated battery consumption (percentage)
    private final ConcurrentHashMap<Long, Double> accumulatedIdleConsumption = new ConcurrentHashMap<>();
    
    // IDLE consumption: ~0.004% per 5 seconds = ~0.048% per minute = ~2.88% per hour
    // This allows 100% battery to last approximately 1.5 days (36 hours) in IDLE state
    // Calculation: 100% / 2.88% per hour = ~34.7 hours ≈ 1.45 days
    // With accumulation, battery will last between 1-2 days depending on usage
    private static final double IDLE_CONSUMPTION_PER_INTERVAL = 0.004; // 0.004% per 5 seconds

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
        // Clear accumulated idle consumption when charging
        accumulatedIdleConsumption.remove(drone.getId());
        
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
     * Trừ ~0.004% mỗi 5s = ~0.048% mỗi phút = ~2.88% mỗi giờ
     * Với 100% pin có thể giữ được khoảng 1.5 ngày (34-36 giờ) ở trạng thái IDLE
     * Sử dụng tích lũy để tránh làm tròn sai (tương tự DELIVERING/RETURNING)
     */
    private void handleIdleConsumption(Drone drone, int currentLevel) {
        if (currentLevel > 0) {
            // Accumulate consumption: ~0.004% per 5 seconds
            // This is much slower than before to allow 1-2 days of battery life
            double accumulated = accumulatedIdleConsumption.getOrDefault(drone.getId(), 0.0);
            accumulated += IDLE_CONSUMPTION_PER_INTERVAL;
            
            // Only deduct battery when accumulated consumption >= 1%
            if (accumulated >= 1.0) {
                int batteryToDeduct = (int) Math.floor(accumulated);
                int newLevel = Math.max(0, currentLevel - batteryToDeduct);
                drone.setBatteryLevel(newLevel);
                accumulated -= batteryToDeduct; // Keep the remainder
                
                // Log when battery decreases significantly
                if (currentLevel % 10 == 0 || newLevel % 10 == 0) {
                    log.debug("⚡ Drone {} idle consumption. Battery: {}% → {}% (accumulated: {:.2f}%)",
                            drone.getSerialNumber(), currentLevel, newLevel, accumulated);
                }
                
                // Cảnh báo khi pin thấp
                if (newLevel <= 20 && currentLevel > 20) {
                    log.warn("⚠️ Drone {} battery is low: {}%. Consider charging.",
                            drone.getSerialNumber(), newLevel);
                }
            } else {
                // No deduction yet, just accumulate
                log.trace("⚡ Drone {} idle consumption accumulating: {:.2f}% (not yet 1%)",
                        drone.getSerialNumber(), accumulated);
            }
            
            // Store accumulated value for next iteration
            accumulatedIdleConsumption.put(drone.getId(), accumulated);
        } else {
            // Clear accumulation when battery is 0
            accumulatedIdleConsumption.remove(drone.getId());
        }
    }
}
