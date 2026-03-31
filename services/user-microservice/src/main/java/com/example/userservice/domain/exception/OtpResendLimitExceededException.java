package com.example.userservice.domain.exception;

public class OtpResendLimitExceededException extends RuntimeException {
    public OtpResendLimitExceededException(int limit) {
        super("OTP resend limit exceeded for today (limit=" + limit + ")");
    }
}

