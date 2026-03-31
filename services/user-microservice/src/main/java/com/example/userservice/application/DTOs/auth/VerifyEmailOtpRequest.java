package com.example.userservice.application.DTOs.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyEmailOtpRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String otp;

    @NotBlank
    private String type;
}

