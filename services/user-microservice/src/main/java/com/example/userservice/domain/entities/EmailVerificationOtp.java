package com.example.userservice.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailVerificationOtp {

    private Long id;
    private Long userId;
    private String email;
    private String otpCode;
    private OtpType type;
    private Instant expiresAt;
    private int attempts;
    private int maxAttempts;
    private OtpStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public enum OtpType {
        SIGNUP,
        CHANGE_EMAIL
    }

    public enum OtpStatus {
        PENDING,
        USED,
        EXPIRED,
        BLOCKED
    }
}

