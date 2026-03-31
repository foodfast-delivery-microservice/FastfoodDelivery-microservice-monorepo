package com.example.userservice.domain.exception;

public class EmailNotVerifiedException extends RuntimeException {

    private final String email;

    public EmailNotVerifiedException(String email) {
        super("Email is not verified: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}

