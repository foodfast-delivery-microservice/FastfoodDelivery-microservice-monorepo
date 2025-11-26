package com.example.demo.domain.exception;

public class ProductDeletionNotAllowedException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Product must be inactive before deletion";

    public ProductDeletionNotAllowedException() {
        super(DEFAULT_MESSAGE);
    }

    public ProductDeletionNotAllowedException(String message) {
        super(message);
    }
}


