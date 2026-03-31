package com.example.userservice.application.DTOs.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeEmailRequest {

    @NotBlank
    @Email
    private String newEmail;
}

