package com.example.droneservice.interfaces.rest;

import com.example.droneservice.application.dto.CreateDroneRequest;
import com.example.droneservice.application.dto.DroneResponse;
import com.example.droneservice.application.usecase.CreateDroneUseCase;
import com.example.droneservice.application.usecase.GetAllDroneUseCase;
import com.example.droneservice.application.usecase.GetDroneByIdUseCase;
import com.example.droneservice.application.usecase.GetDroneByStateUseCase;
import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.State;
import com.example.droneservice.domain.repository.DroneRepository;
import com.example.droneservice.interfaces.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Drone management
 */
@RestController
@RequestMapping("/api/v1/drones")
@RequiredArgsConstructor
public class DroneController {

    private final CreateDroneUseCase createDroneUseCase;
    private final GetAllDroneUseCase getAllDroneUseCase;
    private final GetDroneByStateUseCase  getDroneByStateUseCase;
    private final GetDroneByIdUseCase getDroneByIdUseCase;
    private final DroneRepository droneRepository;

    /**
     * Create a new drone
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DroneResponse>> createDrone(@Valid @RequestBody CreateDroneRequest request) {
        DroneResponse response = createDroneUseCase.execute(request);
        ApiResponse<DroneResponse> result = new ApiResponse<>(
                HttpStatus.CREATED,
                "created drone",
                response,
                null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Get all drones
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DroneResponse>>> getAllDrones() {
        ApiResponse<List<DroneResponse>> result = new ApiResponse<>(
                HttpStatus.OK,
                "got all drones",
                getAllDroneUseCase.execute(),
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
    /**
     * Get drone by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DroneResponse>> getDroneById(@PathVariable Long id) {
        ApiResponse<DroneResponse> result = new ApiResponse<>(
                HttpStatus.OK,
                "got drone by id",
                getDroneByIdUseCase.execute(id),
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /**
     * Get drones by state
     */
    @GetMapping("/state/{state}")
    public ResponseEntity<ApiResponse<List<DroneResponse>>> getDronesByState(@PathVariable State state) {

        ApiResponse<List<DroneResponse>> result = new ApiResponse<>(
                HttpStatus.OK,
                "got drone by state",
                getDroneByStateUseCase.execute(state.toString()),
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }







/*
 * Ukhuc duoi nay chua lm lai
 */
    /**
     * Update drone battery level (for testing/maintenance)
     */
    @PutMapping("/{id}/battery")
    public ResponseEntity<Void> updateBattery(
            @PathVariable Long id,
            @RequestParam Integer level) {

        return droneRepository.findById(id)
                .map(drone -> {
                    if (level < 0 || level > 100) {
                        return ResponseEntity.badRequest().<Void>build();
                    }
                    drone.setBatteryLevel(level);
                    droneRepository.save(drone);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update drone state (for maintenance)
     */
    @PutMapping("/{id}/state")
    public ResponseEntity<Void> updateState(
            @PathVariable Long id,
            @RequestParam State state) {

        return droneRepository.findById(id)
                .map(drone -> {
                    drone.setState(state);
                    droneRepository.save(drone);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
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
