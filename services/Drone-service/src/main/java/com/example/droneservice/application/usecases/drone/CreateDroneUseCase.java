package com.example.droneservice.application.usecase;

import com.example.droneservice.application.dto.CreateDroneRequest;
import com.example.droneservice.application.dto.DroneResponse;
import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.valueobjects.*;
import com.example.droneservice.domain.repository.DroneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        // Create new drone using value objects
        Drone drone = new Drone();
        drone.setSerialNumber(new SerialNumber(request.getSerialNumber()));
        drone.setModel(request.getModel());
        drone.setBatteryLevel(new BatteryLevel(100)); // New drone starts with full battery
        drone.setState(State.IDLE);

        Coordinates baseLocation = new Coordinates(
                request.getBaseLatitude(),
                request.getBaseLongitude());
        drone.setCurrentLocation(baseLocation);
        drone.setBaseLocation(baseLocation);
        drone.setWeightCapacity(new WeightCapacity(request.getWeightCapacity()));

        Drone savedDrone = droneRepository.save(drone);
        log.info("Drone created successfully with ID: {}", savedDrone.getId());

        return mapToResponse(savedDrone);
    }

    private DroneResponse mapToResponse(Drone drone) {
        return DroneResponse.builder()
                .id(drone.getId())
                .serialNumber(drone.getSerialNumber().getValue())
                .model(drone.getModel())
                .batteryLevel(drone.getBatteryLevel().getValue())
                .state(drone.getState())
                .currentLatitude(drone.getCurrentLocation() != null ? drone.getCurrentLocation().getLatitude() : null)
                .currentLongitude(drone.getCurrentLocation() != null ? drone.getCurrentLocation().getLongitude() : null)
                .baseLatitude(drone.getBaseLocation() != null ? drone.getBaseLocation().getLatitude() : null)
                .baseLongitude(drone.getBaseLocation() != null ? drone.getBaseLocation().getLongitude() : null)
                .weightCapacity(drone.getWeightCapacity() != null ? drone.getWeightCapacity().getValue() : null)
                .build();
    }
}
