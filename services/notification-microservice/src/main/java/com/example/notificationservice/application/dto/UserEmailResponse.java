package com.example.notificationservice.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailResponse {

    @NotNull(message = "User ID cannot be null")
    @Positive(message = "User ID must be positive")
    private Long id;

    private String fullName;

    @Email(message = "Email must be a valid email address")
    private String email;

    @Builder.Default
    private Boolean emailUndeliverable = false;
}

