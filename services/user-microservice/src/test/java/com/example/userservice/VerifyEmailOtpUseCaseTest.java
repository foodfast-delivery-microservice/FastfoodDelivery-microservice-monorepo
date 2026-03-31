package com.example.userservice;

import com.example.userservice.application.usecases.auth.VerifyEmailOtpUseCase;
import com.example.userservice.domain.entities.EmailVerificationOtp;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.exception.OtpExpiredException;
import com.example.userservice.domain.exception.OtpInvalidException;
import com.example.userservice.domain.repository.EmailVerificationOtpRepository;
import com.example.userservice.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class VerifyEmailOtpUseCaseTest {

    @Test
    void verify_signup_success_marksUserVerified() {
        EmailVerificationOtpRepository otpRepo = Mockito.mock(EmailVerificationOtpRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        VerifyEmailOtpUseCase useCase = new VerifyEmailOtpUseCase(otpRepo, userRepo);

        EmailVerificationOtp otp = new EmailVerificationOtp();
        otp.setId(1L);
        otp.setUserId(10L);
        otp.setEmail("a@b.com");
        otp.setOtpCode("123456");
        otp.setType(EmailVerificationOtp.OtpType.SIGNUP);
        otp.setStatus(EmailVerificationOtp.OtpStatus.PENDING);
        otp.setExpiresAt(Instant.now().plusSeconds(60));
        otp.setAttempts(0);
        otp.setMaxAttempts(5);

        User user = new User();
        user.setId(10L);
        user.setEmail("a@b.com");
        user.setEmailVerified(false);

        Mockito.when(otpRepo.findActiveByEmailAndType(eq("a@b.com"), eq(EmailVerificationOtp.OtpType.SIGNUP)))
                .thenReturn(Optional.of(otp));
        Mockito.when(userRepo.findById(10L)).thenReturn(Optional.of(user));
        Mockito.when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> useCase.verify("a@b.com", "123456", EmailVerificationOtp.OtpType.SIGNUP));
    }

    @Test
    void verify_expired_throws() {
        EmailVerificationOtpRepository otpRepo = Mockito.mock(EmailVerificationOtpRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        VerifyEmailOtpUseCase useCase = new VerifyEmailOtpUseCase(otpRepo, userRepo);

        EmailVerificationOtp otp = new EmailVerificationOtp();
        otp.setUserId(10L);
        otp.setEmail("a@b.com");
        otp.setOtpCode("123456");
        otp.setType(EmailVerificationOtp.OtpType.SIGNUP);
        otp.setStatus(EmailVerificationOtp.OtpStatus.PENDING);
        otp.setExpiresAt(Instant.now().minusSeconds(1));
        otp.setAttempts(0);
        otp.setMaxAttempts(5);

        Mockito.when(otpRepo.findActiveByEmailAndType(eq("a@b.com"), eq(EmailVerificationOtp.OtpType.SIGNUP)))
                .thenReturn(Optional.of(otp));
        Mockito.when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(OtpExpiredException.class, () -> useCase.verify("a@b.com", "123456", EmailVerificationOtp.OtpType.SIGNUP));
    }

    @Test
    void verify_wrongOtp_throws() {
        EmailVerificationOtpRepository otpRepo = Mockito.mock(EmailVerificationOtpRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        VerifyEmailOtpUseCase useCase = new VerifyEmailOtpUseCase(otpRepo, userRepo);

        EmailVerificationOtp otp = new EmailVerificationOtp();
        otp.setUserId(10L);
        otp.setEmail("a@b.com");
        otp.setOtpCode("123456");
        otp.setType(EmailVerificationOtp.OtpType.SIGNUP);
        otp.setStatus(EmailVerificationOtp.OtpStatus.PENDING);
        otp.setExpiresAt(Instant.now().plusSeconds(60));
        otp.setAttempts(0);
        otp.setMaxAttempts(5);

        Mockito.when(otpRepo.findActiveByEmailAndType(eq("a@b.com"), eq(EmailVerificationOtp.OtpType.SIGNUP)))
                .thenReturn(Optional.of(otp));
        Mockito.when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(OtpInvalidException.class, () -> useCase.verify("a@b.com", "000000", EmailVerificationOtp.OtpType.SIGNUP));
    }
}

