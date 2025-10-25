package com.example.demo.domain.exception;

import jakarta.validation.constraints.NotBlank;

public class EmailAlreadyExistException extends RuntimeException{
    public static final String DEFAULT_MESSAGE = "Email đã tồn tại";

    public EmailAlreadyExistException(@NotBlank String email){
        super(DEFAULT_MESSAGE);
    }
}
