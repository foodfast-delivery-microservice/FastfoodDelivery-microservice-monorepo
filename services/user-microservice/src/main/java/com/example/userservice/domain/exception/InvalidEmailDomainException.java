package com.example.userservice.domain.exception;

public class InvalidEmailDomainException extends RuntimeException {
    public InvalidEmailDomainException(String email) {
        super("The email domain for " + email + " does not exist or cannot receive emails.");
    }
}
