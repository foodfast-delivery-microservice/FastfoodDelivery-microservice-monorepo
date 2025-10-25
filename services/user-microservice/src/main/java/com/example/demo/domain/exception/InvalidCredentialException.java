package com.example.demo.domain.exception;

public class InvalidCredentialException extends RuntimeException{
    public InvalidCredentialException( ) {
        super("Sai mat khau");
    }
}
