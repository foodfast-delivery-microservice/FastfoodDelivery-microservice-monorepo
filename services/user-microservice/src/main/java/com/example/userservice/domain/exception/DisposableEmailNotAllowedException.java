package com.example.userservice.domain.exception;

public class DisposableEmailNotAllowedException extends RuntimeException {
    public DisposableEmailNotAllowedException(String email) {
        super("Disposable email is not allowed: " + email);
    }
}
