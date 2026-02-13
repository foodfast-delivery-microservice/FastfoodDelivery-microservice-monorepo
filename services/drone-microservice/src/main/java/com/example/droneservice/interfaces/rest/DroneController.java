package com.example.droneservice.interfaces.rest;

import com.example.droneservice.application.DTOs.drone.CreateDroneRequest;
import com.example.droneservice.application.DTOs.drone.DroneResponse;
import com.example.droneservice.application.DTOs.drone.UpdateBatteryRequest;
import com.example.droneservice.application.DTOs.drone.UpdateStateRequest;
import com.example.droneservice.application.usecases.drone.*;
import com.example.droneservice.domain.valueobjects.State;
import com.example.droneservice.interfaces.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Drone management
 */
@RestController
@RequestMapping("/api/v1/drones")
@RequiredArgsConstructor
public class DroneController {

    private final CreateDroneUseCase createDroneUseCase;
    private final GetAllDroneUseCase getAllDroneUseCase;
    private final GetDroneByStateUseCase getDroneByStateUseCase;
    private final GetDroneByIdUseCase getDroneByIdUseCase;

    private final UpdateDroneBatteryUseCase updateDroneBatteryUseCase;
    private final UpdateDroneStateUseCase updateDroneStateUseCase;

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
                null);
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
                null);
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
                null);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /**
     * Get drones by state
     */
    @GetMapping("/state/{state}")
    public ResponseEntity<ApiResponse<List<DroneResponse>>> getDronesByState(@PathVariable String state) {

        ApiResponse<List<DroneResponse>> result = new ApiResponse<>(
                HttpStatus.OK,
                "got drone by state",
                getDroneByStateUseCase.execute(state),
                null);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /**
     * Update drone battery level (for testing/maintenance)
     */
    @PutMapping("/{id}/battery")
    public ResponseEntity<ApiResponse<DroneResponse>> updateDroneBattery(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBatteryRequest request) {

        DroneResponse response = updateDroneBatteryUseCase.execute(id, request);
        return ResponseEntity.ok(new ApiResponse<>(
                HttpStatus.OK,
                "Battery level updated successfully",
                response,
                null));
    }

    /**
     * Update drone state (for maintenance)
     */
    @PutMapping("/{id}/state")
    public ResponseEntity<ApiResponse<DroneResponse>> updateDroneState(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStateRequest request) {

        DroneResponse response = updateDroneStateUseCase.execute(id, request);
        return ResponseEntity.ok(new ApiResponse<>(
                HttpStatus.OK,
                "Drone state updated successfully",
                response,
                null));
    }

}
