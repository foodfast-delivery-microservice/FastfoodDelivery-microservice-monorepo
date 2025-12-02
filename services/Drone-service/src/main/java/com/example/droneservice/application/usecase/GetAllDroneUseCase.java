package com.example.droneservice.application.usecase;

import com.example.droneservice.application.dto.DroneResponse;
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
                .map(drone -> new DroneResponse(
                        drone.getId(),
                        drone.getSerialNumber(),
                        drone.getModel(),
                        drone.getBatteryLevel(),
                        drone.getState(),
                        drone.getCurrentLatitude(),
                        drone.getCurrentLongitude(),
                        drone.getBaseLatitude(),
                        drone.getBaseLongitude(),
                        drone.getWeightCapacity()
                ))
                .collect(Collectors.toList());

    }
}
