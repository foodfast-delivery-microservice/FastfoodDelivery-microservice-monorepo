package com.example.droneservice.interfaces.rest;

import com.example.droneservice.application.DTOs.mission.MissionResponse;
import com.example.droneservice.application.DTOs.mission.TrackingResponse;
import com.example.droneservice.application.usecases.mission.*;
import com.example.droneservice.interfaces.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Drone Mission tracking
 */
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class DroneMissionController {

    private final GetAllMissionsUseCase getAllMissionsUseCase;
    private final GetMissionByIdUseCase getMissionByIdUseCase;
    private final GetMissionByOrderIdUseCase getMissionByOrderIdUseCase;
    private final GetMissionsByDroneIdUseCase getMissionsByDroneIdUseCase;
    private final GetMissionTrackingUseCase getMissionTrackingUseCase;

    /**
     * Get all missions
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MissionResponse>>> getAllMissions() {
        ApiResponse<List<MissionResponse>> result = new ApiResponse<>(
                HttpStatus.OK,
                "got all missions",
                getAllMissionsUseCase.execute(),
                null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Get mission by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MissionResponse>> getMissionById(@PathVariable Long id) {
        ApiResponse<MissionResponse> result = new ApiResponse<>(
                HttpStatus.OK,
                "got mission by id",
                getMissionByIdUseCase.execute(id),
                null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Get mission by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<MissionResponse>> getMissionByOrderId(@PathVariable Long orderId) {
        ApiResponse<MissionResponse> result = new ApiResponse<>(
                HttpStatus.OK,
                "got mission by order id",
                getMissionByOrderIdUseCase.execute(orderId),
                null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Get all missions for a specific drone
     */
    @GetMapping("/drone/{droneId}")
    public ResponseEntity<ApiResponse<List<MissionResponse>>> getMissionsByDroneId(@PathVariable Long droneId) {
        ApiResponse<List<MissionResponse>> result = new ApiResponse<>(
                HttpStatus.OK,
                "got missions by drone id",
                getMissionsByDroneIdUseCase.execute(droneId),
                null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Get real-time tracking info for a mission
     */
    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<TrackingResponse>> getTrackingInfo(@PathVariable Long id) {
        ApiResponse<TrackingResponse> result = new ApiResponse<>(
                HttpStatus.OK,
                "got mission tracking info",
                getMissionTrackingUseCase.execute(id),
                null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Get tracking info by order ID (convenience endpoint for frontend)
     */
    @GetMapping("/order/{orderId}/tracking")
    public ResponseEntity<ApiResponse<TrackingResponse>> getTrackingByOrderId(@PathVariable Long orderId) {
        ApiResponse<TrackingResponse> result = new ApiResponse<>(
                HttpStatus.OK,
                "got mission tracking info by order id",
                getMissionTrackingUseCase.executeByOrderId(orderId),
                null);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
