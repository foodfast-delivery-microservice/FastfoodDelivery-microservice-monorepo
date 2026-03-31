package com.example.userservice.infrastructure.persistence.adapter;

import com.example.userservice.domain.entities.EmailVerificationOtp;
import com.example.userservice.domain.entities.EmailVerificationOtp.OtpStatus;
import com.example.userservice.domain.entities.EmailVerificationOtp.OtpType;
import com.example.userservice.domain.repository.EmailVerificationOtpRepository;
import com.example.userservice.infrastructure.persistence.entity.EmailVerificationOtpJpaEntity;
import com.example.userservice.infrastructure.persistence.repository.EmailVerificationOtpJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmailVerificationOtpRepositoryImpl implements EmailVerificationOtpRepository {

    private final EmailVerificationOtpJpaRepository jpaRepository;

    @Override
    public EmailVerificationOtp save(EmailVerificationOtp otp) {
        EmailVerificationOtpJpaEntity entity = toJpaEntity(otp);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        EmailVerificationOtpJpaEntity saved = jpaRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<EmailVerificationOtp> findActiveByEmailAndType(String email, OtpType type) {
        return jpaRepository.findFirstByEmailAndTypeAndStatus(email, type, OtpStatus.PENDING)
                .map(this::toDomainEntity);
    }

    @Override
    public void invalidateAllByUserIdAndType(Long userId, OtpType type) {
        jpaRepository.deleteByUserIdAndType(userId, type);
    }

    @Override
    public long countByUserIdAndTypeCreatedBetween(Long userId, OtpType type, Instant start, Instant end) {
        return jpaRepository.countByUserIdAndTypeAndCreatedAtBetween(userId, type, start, end);
    }

    private EmailVerificationOtpJpaEntity toJpaEntity(EmailVerificationOtp otp) {
        EmailVerificationOtpJpaEntity entity = new EmailVerificationOtpJpaEntity();
        entity.setId(otp.getId());
        entity.setUserId(otp.getUserId());
        entity.setEmail(otp.getEmail());
        entity.setOtpCode(otp.getOtpCode());
        entity.setType(otp.getType());
        entity.setExpiresAt(otp.getExpiresAt());
        entity.setAttempts(otp.getAttempts());
        entity.setMaxAttempts(otp.getMaxAttempts());
        entity.setStatus(otp.getStatus());
        entity.setCreatedAt(otp.getCreatedAt());
        entity.setUpdatedAt(otp.getUpdatedAt());
        return entity;
    }

    private EmailVerificationOtp toDomainEntity(EmailVerificationOtpJpaEntity entity) {
        EmailVerificationOtp otp = new EmailVerificationOtp();
        otp.setId(entity.getId());
        otp.setUserId(entity.getUserId());
        otp.setEmail(entity.getEmail());
        otp.setOtpCode(entity.getOtpCode());
        otp.setType(entity.getType());
        otp.setExpiresAt(entity.getExpiresAt());
        otp.setAttempts(entity.getAttempts());
        otp.setMaxAttempts(entity.getMaxAttempts());
        otp.setStatus(entity.getStatus());
        otp.setCreatedAt(entity.getCreatedAt());
        otp.setUpdatedAt(entity.getUpdatedAt());
        return otp;
    }
}

