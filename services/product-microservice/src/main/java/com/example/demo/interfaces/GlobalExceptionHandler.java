package com.example.demo.interfaces;

import com.example.demo.domain.exception.InvalidCategoryException;
import com.example.demo.domain.exception.InvalidNameException;
import com.example.demo.interfaces.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidCategoryException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCategoryException(InvalidCategoryException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                null,
                "CATEGORIES HAVE TO BE FOOD OR DRINK"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InvalidNameException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidNameException(InvalidNameException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                null,
                "INVALID_NAME"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
