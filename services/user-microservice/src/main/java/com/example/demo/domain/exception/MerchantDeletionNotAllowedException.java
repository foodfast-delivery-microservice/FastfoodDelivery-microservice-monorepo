package com.example.demo.domain.exception;

public class MerchantDeletionNotAllowedException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Merchant must be inactive (blocked) before deletion";

    public MerchantDeletionNotAllowedException() {
        super(DEFAULT_MESSAGE);
    }

    public MerchantDeletionNotAllowedException(String message) {
        super(message);
    }
}


























