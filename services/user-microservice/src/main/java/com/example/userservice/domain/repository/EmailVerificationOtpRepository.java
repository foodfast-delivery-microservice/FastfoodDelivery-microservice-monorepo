package com.example.userservice.domain.repository;

import com.example.userservice.domain.entities.EmailVerificationOtp;
import com.example.userservice.domain.entities.EmailVerificationOtp.OtpType;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationOtpRepository {

    EmailVerificationOtp save(EmailVerificationOtp otp);

    Optional<EmailVerificationOtp> findActiveByEmailAndType(String email, OtpType type);

    void invalidateAllByUserIdAndType(Long userId, OtpType type);

    long countByUserIdAndTypeCreatedBetween(Long userId, OtpType type, Instant start, Instant end);
}

