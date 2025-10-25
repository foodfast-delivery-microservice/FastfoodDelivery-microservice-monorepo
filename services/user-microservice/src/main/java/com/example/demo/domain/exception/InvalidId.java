package com.example.demo.domain.exception;

public class InvalidId extends RuntimeException{
    public static final String DEFAULT_MESSGAGE = "invalid ID";
    public InvalidId(Long id) {
        super(DEFAULT_MESSGAGE + ": " + id);
    }
}
