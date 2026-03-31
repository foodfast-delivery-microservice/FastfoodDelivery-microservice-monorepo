package com.example.userservice.infrastructure.persistence.entity;

import com.example.userservice.domain.entities.EmailVerificationOtp.OtpStatus;
import com.example.userservice.domain.entities.EmailVerificationOtp.OtpType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "email_verification_otp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationOtpJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 20)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OtpType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OtpStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

