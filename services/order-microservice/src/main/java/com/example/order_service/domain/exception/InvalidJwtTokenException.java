package com.example.order_service.domain.exception;

/**
 * Exception thrown when JWT token is invalid or cannot be parsed
 */
public class InvalidJwtTokenException extends OrderValidationException {

    public InvalidJwtTokenException(String message) {
        super(message);
    }

    public InvalidJwtTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

