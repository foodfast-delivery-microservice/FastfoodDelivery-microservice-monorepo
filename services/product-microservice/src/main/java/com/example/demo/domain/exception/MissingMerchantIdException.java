package com.example.demo.domain.exception;

public class MissingMerchantIdException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "merchantId parameter is required for admin";

    public MissingMerchantIdException() {
        super(DEFAULT_MESSAGE);
    }

    public MissingMerchantIdException(String message) {
        super(message);
    }
}