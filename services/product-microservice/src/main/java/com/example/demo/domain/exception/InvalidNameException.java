package com.example.demo.domain.exception;

public class InvalidNameException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Invalid name!";
    public InvalidNameException() {
        super(DEFAULT_MESSAGE);
    }
}
