package com.example.userservice.application.DTOs.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationOtpRequestedEvent {
    private Long userId;
    private String email;
    private String otpCode;
    private String type;
}

