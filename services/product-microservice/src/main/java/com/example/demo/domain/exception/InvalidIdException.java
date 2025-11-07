package com.example.demo.domain.exception;

public class InvalidIdException extends RuntimeException{
    private static final String   DEFAULT_MESSAGE = "Invalid ID";
    public InvalidIdException(String message) {
        super(message + ": "+ DEFAULT_MESSAGE);
    }
}
