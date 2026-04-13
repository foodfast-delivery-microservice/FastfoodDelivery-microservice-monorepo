package com.example.droneservice.application.usecases.drone;

import com.example.droneservice.application.DTOs.drone.DroneResponse;
import com.example.droneservice.domain.exception.InvalidId;
import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.repository.DroneRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetDroneByIdUseCase {
    private final DroneRepository droneRepository;

    public DroneResponse execute(Long id) {
        Drone drone = droneRepository.findById(id)
                .orElseThrow(() -> new InvalidId(id));
        return DroneResponse.fromEntity(drone);
    }

}
