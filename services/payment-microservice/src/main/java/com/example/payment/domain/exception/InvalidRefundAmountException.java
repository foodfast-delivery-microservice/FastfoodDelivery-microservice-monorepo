package com.example.payment.domain.exception;

/**
 * Exception thrown when refund amount is invalid
 */
public class InvalidRefundAmountException extends PaymentValidationException {

    public InvalidRefundAmountException(String message) {
        super(message);
    }

    public InvalidRefundAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}





