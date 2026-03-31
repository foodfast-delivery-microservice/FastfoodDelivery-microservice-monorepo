package com.example.userservice.domain.exception;

public class OtpExpiredException extends RuntimeException {
    public OtpExpiredException() {
        super("OTP is expired");
    }
}

