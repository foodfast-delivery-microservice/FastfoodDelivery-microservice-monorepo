package com.example.droneservice.interfaces.rest;

import com.example.droneservice.application.dto.AssignDroneRequest;
import com.example.droneservice.application.dto.MissionResponse;
import com.example.droneservice.application.usecase.AssignDroneToOrderUseCase;
import com.example.droneservice.interfaces.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for manual drone assignment
 */
@RestController
@RequestMapping("/api/v1/drones/assignments")
@RequiredArgsConstructor
@Slf4j
public class DroneAssignmentController {

    private final AssignDroneToOrderUseCase assignDroneUseCase;

    /**
     * Manually assign a drone to an order
     * POST /api/v1/assignments
     * 
     * Use case: Admin hoặc System có thể manually assign drone cho order
     * thay vì đợi auto-assignment từ event
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MissionResponse>> assignDroneToOrder(
            @Valid @RequestBody AssignDroneRequest request) {

        log.info("Manual drone assignment request for order: {}", request.getOrderId());

        try {
            MissionResponse mission = assignDroneUseCase.execute(request);

            ApiResponse<MissionResponse> response = new ApiResponse<>(
                    HttpStatus.CREATED,
                    "Drone assigned successfully",
                    mission,
                    null);

            log.info("✅ Drone {} assigned to order {}. Mission ID: {}",
                    mission.getDroneSerialNumber(),
                    request.getOrderId(),
                    mission.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalStateException e) {
            log.error("❌ Failed to assign drone: {}", e.getMessage());

            ApiResponse<MissionResponse> response = new ApiResponse<>(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    null,
                    null);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
