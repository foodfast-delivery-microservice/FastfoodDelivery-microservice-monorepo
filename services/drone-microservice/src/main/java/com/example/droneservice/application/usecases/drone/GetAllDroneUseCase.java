package com.example.droneservice.application.usecases.drone;

import com.example.droneservice.application.DTOs.drone.DroneResponse;
import com.example.droneservice.domain.repository.DroneRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetAllDroneUseCase {
    private final DroneRepository droneRepository;

    public List<DroneResponse> execute() {
        return droneRepository.findAll()
                .stream()
                .map(DroneResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
