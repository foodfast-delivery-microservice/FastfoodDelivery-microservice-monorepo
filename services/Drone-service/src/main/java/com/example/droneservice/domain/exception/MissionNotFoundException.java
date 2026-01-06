package com.example.droneservice.domain.exception;

public class MissionNotFoundException extends RuntimeException {
    public static final String DEFAULT_MESSGAGE = " Mission does not exist";
    public MissionNotFoundException(Long id) {
        super(DEFAULT_MESSGAGE+id);
    }
}

