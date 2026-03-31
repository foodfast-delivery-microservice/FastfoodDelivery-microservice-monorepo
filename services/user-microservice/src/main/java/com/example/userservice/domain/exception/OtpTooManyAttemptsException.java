package com.example.userservice.domain.exception;

public class OtpTooManyAttemptsException extends RuntimeException {
    public OtpTooManyAttemptsException() {
        super("OTP attempts exceeded");
    }
}

