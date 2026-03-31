package com.example.userservice.infrastructure.persistence.repository;

import com.example.userservice.domain.entities.EmailVerificationOtp.OtpStatus;
import com.example.userservice.domain.entities.EmailVerificationOtp.OtpType;
import com.example.userservice.infrastructure.persistence.entity.EmailVerificationOtpJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface EmailVerificationOtpJpaRepository extends JpaRepository<EmailVerificationOtpJpaEntity, Long> {

    Optional<EmailVerificationOtpJpaEntity> findFirstByEmailAndTypeAndStatus(String email, OtpType type, OtpStatus status);

    void deleteByUserIdAndType(Long userId, OtpType type);

    long countByUserIdAndTypeAndCreatedAtBetween(Long userId, OtpType type, Instant start, Instant end);
}

