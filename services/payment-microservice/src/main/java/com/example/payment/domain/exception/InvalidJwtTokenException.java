package com.example.payment.domain.exception;

/**
 * Exception thrown when JWT token is invalid or cannot be parsed
 */
public class InvalidJwtTokenException extends PaymentValidationException {

    public InvalidJwtTokenException(String message) {
        super(message);
    }

    public InvalidJwtTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

