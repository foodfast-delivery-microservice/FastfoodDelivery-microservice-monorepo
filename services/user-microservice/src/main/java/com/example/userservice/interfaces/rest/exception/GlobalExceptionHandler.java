package com.example.userservice.interfaces.rest.exception;

import com.example.userservice.domain.exception.*;
import com.example.userservice.interfaces.common.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyExistException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExistException(EmailAlreadyExistException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                null,
                "EMAIL_ALREADY_EXISTS");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DisposableEmailNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisposableEmailNotAllowedException(DisposableEmailNotAllowedException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                null,
                "DISPOSABLE_EMAIL_NOT_ALLOWED");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                null,
                "NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                null,
                "INTERNAL_SERVER_ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(AdminAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AdminAccessDeniedException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                null,
                "FORBIDDEN");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(UserNotFoundException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                null,
                "NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidId.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidIdException(InvalidId ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                null,
                "INVALID_ID");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentialException(InvalidCredentialException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                null,
                "UNAUTHORIZED");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleUsernameAlreadyExistException(UsernameAlreadyExistException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                null,
                "USER_ALREADY_EXISTS");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        java.util.Map<String, String> data = java.util.Map.of("email", ex.getEmail());
        ApiResponse<java.util.Map<String, String>> response = new ApiResponse<>(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                data,
                "EMAIL_NOT_VERIFIED");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({OtpInvalidException.class, OtpExpiredException.class, OtpTooManyAttemptsException.class})
    public ResponseEntity<ApiResponse<Void>> handleOtpExceptions(RuntimeException ex) {
        String code = ex instanceof OtpExpiredException
                ? "OTP_EXPIRED"
                : ex instanceof OtpTooManyAttemptsException
                        ? "OTP_TOO_MANY_ATTEMPTS"
                        : "OTP_INVALID";

        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                null,
                code);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(OtpResendLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleOtpResendLimit(OtpResendLimitExceededException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.TOO_MANY_REQUESTS,
                ex.getMessage(),
                null,
                "OTP_RESEND_LIMIT_EXCEEDED");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.BAD_REQUEST,
                errorMessage,
                null,
                "VALIDATION_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
