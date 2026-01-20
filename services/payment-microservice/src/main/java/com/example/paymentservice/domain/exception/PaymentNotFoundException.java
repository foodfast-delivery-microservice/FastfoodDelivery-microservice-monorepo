package com.example.paymentservice.domain.exception;

/**
 * Exception thrown when payment is not found
 */
public class PaymentNotFoundException extends PaymentValidationException {

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}





