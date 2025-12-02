package com.example.droneservice.application.usecase;

import com.example.droneservice.application.dto.DroneResponse;
import com.example.droneservice.domain.exception.InvalidId;
import com.example.droneservice.domain.model.Drone;
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
