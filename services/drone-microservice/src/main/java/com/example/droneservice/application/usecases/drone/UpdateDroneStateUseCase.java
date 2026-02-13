package com.example.droneservice.application.usecases.drone;

import com.example.droneservice.application.DTOs.drone.DroneResponse;
import com.example.droneservice.application.DTOs.drone.UpdateStateRequest;
import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.exception.DroneNotFoundException;
import com.example.droneservice.domain.repository.DroneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Use case for updating drone state
 */
@Service
@RequiredArgsConstructor
public class UpdateDroneStateUseCase {
    private final DroneRepository droneRepository;

    public DroneResponse execute(Long droneId, UpdateStateRequest request) {
        Drone drone = droneRepository.findById(droneId)
                .orElseThrow(() -> new DroneNotFoundException(droneId));

        // DTO already validated that state is not null
        // Business rule: validate state transition if needed
        if (!drone.getState().canTransitionTo(request.getState())) {
            throw new IllegalStateException(
                    "Cannot transition from " + drone.getState() + " to " + request.getState());
        }

        drone.setState(request.getState());
        Drone savedDrone = droneRepository.save(drone);
        return DroneResponse.fromEntity(savedDrone);
    }
}
