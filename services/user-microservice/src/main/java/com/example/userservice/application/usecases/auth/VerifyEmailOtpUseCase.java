package com.example.userservice.application.usecases.auth;

import com.example.userservice.domain.entities.EmailVerificationOtp;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.exception.OtpExpiredException;
import com.example.userservice.domain.exception.OtpInvalidException;
import com.example.userservice.domain.exception.OtpTooManyAttemptsException;
import com.example.userservice.domain.repository.EmailVerificationOtpRepository;
import com.example.userservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyEmailOtpUseCase {

    private final EmailVerificationOtpRepository emailVerificationOtpRepository;
    private final UserRepository userRepository;

    @Transactional
    public void verify(String email, String otpCode, EmailVerificationOtp.OtpType type) {
        log.info("Verifying OTP for email: {}, type: {}, code: {}", email, type, otpCode);
        EmailVerificationOtp otp = emailVerificationOtpRepository.findActiveByEmailAndType(email, type)
                .orElseThrow(OtpInvalidException::new);

        if (otp.getStatus() != EmailVerificationOtp.OtpStatus.PENDING) {
            throw new OtpInvalidException();
        }

        if (otp.getExpiresAt() == null || otp.getExpiresAt().isBefore(Instant.now())) {
            otp.setStatus(EmailVerificationOtp.OtpStatus.EXPIRED);
            emailVerificationOtpRepository.save(otp);
            throw new OtpExpiredException();
        }

        if (otp.getAttempts() >= otp.getMaxAttempts()) {
            otp.setStatus(EmailVerificationOtp.OtpStatus.BLOCKED);
            emailVerificationOtpRepository.save(otp);
            throw new OtpTooManyAttemptsException();
        }

        if (!otp.getOtpCode().equals(otpCode)) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= otp.getMaxAttempts()) {
                otp.setStatus(EmailVerificationOtp.OtpStatus.BLOCKED);
            }
            emailVerificationOtpRepository.save(otp);
            throw new OtpInvalidException();
        }

        // success
        otp.setStatus(EmailVerificationOtp.OtpStatus.USED);
        emailVerificationOtpRepository.save(otp);

        User user = userRepository.findById(otp.getUserId())
                .orElseThrow(() -> new com.example.userservice.domain.exception.InvalidId(otp.getUserId()));

        if (type == EmailVerificationOtp.OtpType.SIGNUP) {
            user.setEmailVerified(true);
        } else if (type == EmailVerificationOtp.OtpType.CHANGE_EMAIL) {
            user.setEmail(otp.getEmail());
            user.setPendingEmail(null);
            user.setEmailVerified(true);
        }

        userRepository.save(user);
    }
}

