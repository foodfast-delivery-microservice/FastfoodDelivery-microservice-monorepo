package com.example.demo.domain.exception;

public class UsernameAlreadyExistException extends RuntimeException{
    private static final String DEFAULT_MESSAGE = "username đã tồn tại";

    public UsernameAlreadyExistException(String username) {
        super(DEFAULT_MESSAGE + username);
    }
}
