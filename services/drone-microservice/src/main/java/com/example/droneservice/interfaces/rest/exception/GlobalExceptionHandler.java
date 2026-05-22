package com.example.droneservice.interfaces.rest.exception;

import com.example.droneservice.domain.exception.*;
import com.example.droneservice.interfaces.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DroneIdAlreadyExisted.class)
    public ResponseEntity<ApiResponse<Object>> handleDroneIdAlreadyExisted(DroneIdAlreadyExisted ex) {
        // Tạo response theo format chuẩn của bạn
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.CONFLICT, // 409 Conflict hợp lý cho lỗi trùng lặp
                ex.getMessage(),     // "This ID existed"
                null,                // Data là null
                "DRONE_ID_DUPLICATE" // Hoặc null, tùy field cuối của bạn là gì
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DroneNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleDroneNotFoundException(DroneNotFoundException ex) {
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                null,
                "DRONE_NOT_FOUND"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidId.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidId(InvalidId ex) {
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                null,
                "INVALID_ID"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidBattery.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidBattery(InvalidBattery ex) {
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                null,
                "INVALID_BATTERY"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MissionNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissionNotFoundException(MissionNotFoundException ex) {
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                null,
                "MISSION_NOT_FOUND"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
                .error("Unexpected error", ex);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                null,
                "INTERNAL_SERVER_ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
