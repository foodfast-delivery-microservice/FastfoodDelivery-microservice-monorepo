package com.example.userservice.interfaces.rest;

import com.example.userservice.application.usecases.user.GetUserByIdUseCase;
import com.example.userservice.application.usecases.user.UpdateEmailDeliverabilityUseCase;
import com.example.userservice.interfaces.common.ApiResponse;
import com.example.userservice.application.DTOs.user.CreateUserResponse;
import com.example.userservice.application.DTOs.user.UpdateEmailDeliverabilityRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API controller for service-to-service calls
 * These endpoints do not require authentication and are only for internal microservice communication
 */
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final GetUserByIdUseCase getUserByIdUseCase;
    private final UpdateEmailDeliverabilityUseCase updateEmailDeliverabilityUseCase;

    /**
     * Validate user by user ID (Internal API - no authentication required)
     * GET /api/internal/users/{id}/validate
     * 
     * This endpoint is used by other microservices (e.g., payment-service, order-service) 
     * to validate if a user exists and is active
     */
    @GetMapping("/{id}/validate")
    public ResponseEntity<ApiResponse<CreateUserResponse>> validateUserInternal(@PathVariable Long id) {
        log.info("Internal API: Validating user with id: {}", id);

        try {
            CreateUserResponse getUser = getUserByIdUseCase.execute(id);

            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.OK,
                    "user validated",
                    getUser,
                    null
            );
            log.info("Internal API: User {} validated successfully", id);
            return ResponseEntity.ok(result);
        } catch (com.example.userservice.domain.exception.InvalidId ex) {
            // User not found - return 404
            log.warn("Internal API: User {} not found", id);
            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.NOT_FOUND,
                    ex.getMessage(),
                    null,
                    "INVALID_ID"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        } catch (Exception ex) {
            // Other errors - return 500
            log.error("Internal API: Error validating user {}", id, ex);
            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error validating user: " + ex.getMessage(),
                    null,
                    "INTERNAL_SERVER_ERROR"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PatchMapping("/{id}/deliverability")
    public ResponseEntity<ApiResponse<CreateUserResponse>> updateEmailDeliverability(
            @PathVariable Long id,
            @RequestBody UpdateEmailDeliverabilityRequest request
    ) {
        log.info("Internal API: Updating email deliverability for userId={}, undeliverable={}",
                id, request.getUndeliverable());

        try {
            com.example.userservice.domain.entities.User updatedUser = updateEmailDeliverabilityUseCase.execute(id, request);
            CreateUserResponse response = CreateUserResponse.fromEntity(updatedUser);
            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.OK,
                    "user email deliverability updated",
                    response,
                    null
            );
            return ResponseEntity.ok(result);
        } catch (com.example.userservice.domain.exception.InvalidId ex) {
            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.NOT_FOUND,
                    ex.getMessage(),
                    null,
                    "INVALID_ID"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        } catch (Exception ex) {
            log.error("Internal API: Error updating user deliverability for userId={}", id, ex);
            ApiResponse<CreateUserResponse> result = new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error updating user deliverability: " + ex.getMessage(),
                    null,
                    "INTERNAL_SERVER_ERROR"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}

