package com.example.userservice.application.service;

import com.example.userservice.application.DTOs.event.EmailVerificationOtpRequestedEvent;
import com.example.userservice.domain.entities.EmailVerificationOtp;
import com.example.userservice.domain.entities.OutboxEvent;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.exception.OtpResendLimitExceededException;
import com.example.userservice.domain.repository.EmailVerificationOtpRepository;
import com.example.userservice.domain.repository.OutboxEventRepository;
import com.example.userservice.domain.valueobjects.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOtpService {

    private final EmailVerificationOtpRepository emailVerificationOtpRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final EventPayloadSerializer eventPayloadSerializer;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.email.ttl-minutes:10}")
    private long otpTtlMinutes;

    @Value("${app.otp.email.max-attempts:5}")
    private int otpMaxAttempts;

    @Value("${app.otp.email.max-resends-per-day:5}")
    private int maxResendsPerDay;

    public EmailVerificationOtp generateForSignup(User user) {
        enforceDailyResendLimit(user.getId(), EmailVerificationOtp.OtpType.SIGNUP);
        emailVerificationOtpRepository.invalidateAllByUserIdAndType(user.getId(), EmailVerificationOtp.OtpType.SIGNUP);

        String otpCode = generateOtpCode();

        EmailVerificationOtp otp = new EmailVerificationOtp();
        otp.setUserId(user.getId());
        otp.setEmail(user.getEmail());
        otp.setOtpCode(otpCode);
        otp.setType(EmailVerificationOtp.OtpType.SIGNUP);
        otp.setExpiresAt(Instant.now().plus(otpTtlMinutes, ChronoUnit.MINUTES));
        otp.setAttempts(0);
        otp.setMaxAttempts(otpMaxAttempts);
        otp.setStatus(EmailVerificationOtp.OtpStatus.PENDING);

        EmailVerificationOtp saved = emailVerificationOtpRepository.save(otp);
        createOtpRequestedOutboxEvent(saved);
        return saved;
    }

    public EmailVerificationOtp generateForChangeEmail(User user, String newEmail) {
        enforceDailyResendLimit(user.getId(), EmailVerificationOtp.OtpType.CHANGE_EMAIL);
        emailVerificationOtpRepository.invalidateAllByUserIdAndType(user.getId(), EmailVerificationOtp.OtpType.CHANGE_EMAIL);

        String otpCode = generateOtpCode();

        EmailVerificationOtp otp = new EmailVerificationOtp();
        otp.setUserId(user.getId());
        otp.setEmail(newEmail);
        otp.setOtpCode(otpCode);
        otp.setType(EmailVerificationOtp.OtpType.CHANGE_EMAIL);
        otp.setExpiresAt(Instant.now().plus(otpTtlMinutes, ChronoUnit.MINUTES));
        otp.setAttempts(0);
        otp.setMaxAttempts(otpMaxAttempts);
        otp.setStatus(EmailVerificationOtp.OtpStatus.PENDING);

        EmailVerificationOtp saved = emailVerificationOtpRepository.save(otp);
        createOtpRequestedOutboxEvent(saved);
        return saved;
    }

    private String generateOtpCode() {
        int value = secureRandom.nextInt(1_000_000);
        String code = String.format("%06d", value);
        log.info("Generated OTP Code: {}", code);
        return code;
    }

    private void enforceDailyResendLimit(Long userId, EmailVerificationOtp.OtpType type) {
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zoneId);
        Instant start = today.atStartOfDay(zoneId).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zoneId).toInstant();
        long sentToday = emailVerificationOtpRepository.countByUserIdAndTypeCreatedBetween(userId, type, start, end);
        if (sentToday >= maxResendsPerDay) {
            throw new OtpResendLimitExceededException(maxResendsPerDay);
        }
    }

    private void createOtpRequestedOutboxEvent(EmailVerificationOtp otp) {
        EmailVerificationOtpRequestedEvent eventDto = EmailVerificationOtpRequestedEvent.builder()
                .userId(otp.getUserId())
                .email(otp.getEmail())
                .otpCode(otp.getOtpCode())
                .type(otp.getType().name())
                .build();

        String payloadJson = eventPayloadSerializer.serialize(eventDto);

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("User")
                .aggregateId(otp.getUserId().toString())
                .type("EmailVerificationOtpRequested")
                .payload(payloadJson)
                .status(EventStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        outboxEventRepository.save(event);
    }
}

