package com.example.demo.domain.exception;

public class InvalidCategoryException extends RuntimeException {
    private static final String   DEFAULT_MESSAGE = "Invalid Category";
    public InvalidCategoryException(String message) {
        super(message+ ": "+ DEFAULT_MESSAGE);
    }
}
