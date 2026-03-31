package com.example.userservice.domain.exception;

public class OtpInvalidException extends RuntimeException {
    public OtpInvalidException() {
        super("OTP is invalid");
    }
}

