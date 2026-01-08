package com.example.droneservice.domain.exception;

public class DroneIdAlreadyExisted extends RuntimeException {
    public static final String DEFAULT_MESSGAGE = "This ID existed";
    public DroneIdAlreadyExisted() {
        super(DEFAULT_MESSGAGE );
    }
}
