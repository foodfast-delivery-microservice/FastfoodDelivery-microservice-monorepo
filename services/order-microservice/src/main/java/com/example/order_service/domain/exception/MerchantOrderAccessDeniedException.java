package com.example.order_service.domain.exception;

public class MerchantOrderAccessDeniedException extends RuntimeException {

    public MerchantOrderAccessDeniedException(String message) {
        super(message);
    }

    public MerchantOrderAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}

