package com.example.droneservice.application.usecases.drone;

import com.example.droneservice.application.DTOs.drone.DroneResponse;
import com.example.droneservice.application.DTOs.drone.UpdateBatteryRequest;
import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.exception.DroneNotFoundException;
import com.example.droneservice.domain.repository.DroneRepository;
import com.example.droneservice.domain.valueobjects.BatteryLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Use case for updating drone battery level
 */
@Service
@RequiredArgsConstructor
public class UpdateDroneBatteryUseCase {
    private final DroneRepository droneRepository;

    public DroneResponse execute(Long droneId, UpdateBatteryRequest request) {
        Drone drone = droneRepository.findById(droneId)
                .orElseThrow(() -> new DroneNotFoundException(droneId));

        // DTO already validated input (0-100)
        // BatteryLevel constructor will validate domain rules
        drone.setBatteryLevel(new BatteryLevel(request.getBatteryLevel()));

        Drone savedDrone = droneRepository.save(drone);
        return DroneResponse.fromEntity(savedDrone);
    }
}
