package com.example.droneservice.domain.exception;

public class DroneNotFoundException extends RuntimeException {
    public static final String DEFAULT_MESSGAGE = " Drone does not exist";
    public DroneNotFoundException (Long id) {
        super(DEFAULT_MESSGAGE+id);
    }
}
