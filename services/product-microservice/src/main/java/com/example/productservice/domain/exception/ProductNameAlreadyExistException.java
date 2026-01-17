package com.example.productservice.domain.exception;

public class ProductNameAlreadyExistException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Product name already exists";

    public ProductNameAlreadyExistException(String message) {
        super(message + ": " + DEFAULT_MESSAGE);
    }
}
