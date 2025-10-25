package com.example.demo.domain.exception;

public class InvalidRoleException extends RuntimeException{
    public static final String DEFAULT_MESSGAGE = "invalid role";

    public InvalidRoleException(String role){
        super(DEFAULT_MESSGAGE + ": " + role);
    }

}
