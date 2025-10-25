package com.example.demo.interfaces;

import com.example.demo.domain.exception.*;
import com.example.demo.interfaces.rest.dto.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyExistException.class)
    public ResponseEntity<ApiResponse<Void>>  handleEmailAlreadyExistException(EmailAlreadyExistException ex){
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                null,
                "EMAIL_ALREADY_EXISTS"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex){
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                null,
                "NOT_FOUND"
        );
        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex){
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                null,
                "INTERNAL_SERVER_ERROR"
        );
        return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(AdminAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AdminAccessDeniedException ex){
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                null,
                "FORBIDDEN"
        );
        return  ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(UserNotFoundException ex){
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                null,
                "NOT_FOUND"
        );
        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentialException(InvalidCredentialException ex){
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                null,
                "UNAUTHORIZED"
        );
        return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}
