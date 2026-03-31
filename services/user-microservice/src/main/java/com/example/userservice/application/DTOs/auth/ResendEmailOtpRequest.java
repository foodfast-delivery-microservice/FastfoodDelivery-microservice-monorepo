package com.example.userservice.application.DTOs.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendEmailOtpRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String type; // SIGNUP | CHANGE_EMAIL
}

