package com.example.droneservice.domain.exception;

public class InvalidBattery extends RuntimeException {
    public InvalidBattery(String message) {
        super(message);
    }
}
