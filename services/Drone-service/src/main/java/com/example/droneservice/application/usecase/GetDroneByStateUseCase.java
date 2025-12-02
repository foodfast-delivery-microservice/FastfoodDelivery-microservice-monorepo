package com.example.droneservice.application.usecase;

import com.example.droneservice.application.dto.DroneResponse;
import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.State;
import com.example.droneservice.domain.repository.DroneRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetDroneByStateUseCase {
    private final DroneRepository droneRepository;

    public List<DroneResponse> execute(String state) {

        State states;
        try {
            states = State.valueOf(state.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid drone state" + state);
        }
        List<Drone> droneByState = droneRepository.findByState(states);
        return droneByState
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
