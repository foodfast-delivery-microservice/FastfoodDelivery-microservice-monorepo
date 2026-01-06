package com.example.droneservice.application.usecases.drone;

import com.example.droneservice.application.DTOs.drone.DroneResponse;
import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.valueobjects.State;
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
            states = State.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid drone state: " + state);
        }
        List<Drone> droneByState = droneRepository.findByState(states);
        return droneByState
                .stream()
                .map(DroneResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
