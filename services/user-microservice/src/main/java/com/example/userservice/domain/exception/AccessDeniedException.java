package com.example.userservice.domain.exception;

/**
 * Domain exception thrown when a user attempts to access a resource
 * they don't have permission to access.
 */
public class AccessDeniedException extends RuntimeException {
    
    public AccessDeniedException(String message) {
        super(message);
    }
    
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
