package com.example.demo.domain.exception;

public class AccessDeniedException extends RuntimeException{
    private static final String DEFAULT_MESSAGE = "access denied";
    public AccessDeniedException() {
        super(DEFAULT_MESSAGE);
    }

}
