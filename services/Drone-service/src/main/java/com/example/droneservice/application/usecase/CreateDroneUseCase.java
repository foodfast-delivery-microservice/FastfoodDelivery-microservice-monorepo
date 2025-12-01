package com.example.droneservice.application.usecase;

import com.example.droneservice.application.dto.CreateDroneRequest;
import com.example.droneservice.application.dto.DroneResponse;
import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.State;
import com.example.droneservice.domain.repository.DroneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Slf4j
public class CreateDroneUseCase {

    private final DroneRepository droneRepository;

    @Transactional
    public DroneResponse execute(CreateDroneRequest request) {
        log.info("Creating new drone with serial number: {}", request.getSerialNumber());

        // Validate serial number uniqueness
        if (droneRepository.findBySerialNumber(request.getSerialNumber()).isPresent()) {
            throw new IllegalArgumentException(
                    "Drone with serial number " + request.getSerialNumber() + " already exists");
        }

        // Create new drone
        Drone drone = new Drone();
        drone.setSerialNumber(request.getSerialNumber());
        drone.setModel(request.getModel());
        drone.setBatteryLevel(100); // New drone starts with full battery
        drone.setState(State.IDLE);
        drone.setCurrentLatitude(request.getBaseLatitude());
        drone.setCurrentLongitude(request.getBaseLongitude());
        drone.setBaseLatitude(request.getBaseLatitude());
        drone.setBaseLongitude(request.getBaseLongitude());
        drone.setWeightCapacity(request.getWeightCapacity());

        Drone savedDrone = droneRepository.save(drone);
        log.info("Drone created successfully with ID: {}", savedDrone.getId());

        return mapToResponse(savedDrone);
    }

    private DroneResponse mapToResponse(Drone drone) {
        return DroneResponse.builder()
                .id(drone.getId())
                .serialNumber(drone.getSerialNumber())
                .model(drone.getModel())
                .batteryLevel(drone.getBatteryLevel())
                .state(drone.getState())
                .currentLatitude(drone.getCurrentLatitude())
                .currentLongitude(drone.getCurrentLongitude())
                .baseLatitude(drone.getBaseLatitude())
                .baseLongitude(drone.getBaseLongitude())
                .weightCapacity(drone.getWeightCapacity())
                .build();
    }
}
