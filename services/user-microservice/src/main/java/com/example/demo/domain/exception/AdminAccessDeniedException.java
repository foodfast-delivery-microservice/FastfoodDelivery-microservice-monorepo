package com.example.demo.domain.exception;

public class AdminAccessDeniedException extends RuntimeException {
    public AdminAccessDeniedException() {
        super("Only admin can update roles");
    }

    public AdminAccessDeniedException(String message) {
        super(message);
    }
}