package com.example.userservice.application.DTOs.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserForgotPasswordEvent {
    private String email;
    private String resetToken;
    private LocalDateTime requestedAt;
}
