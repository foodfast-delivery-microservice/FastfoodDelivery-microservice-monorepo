package com.example.userservice.application.usecases.auth;

import com.example.userservice.application.service.EmailOtpService;
import com.example.userservice.domain.entities.EmailVerificationOtp;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResendEmailOtpUseCase {

    private final UserRepository userRepository;
    private final EmailOtpService emailOtpService;

    @Transactional
    public void resend(String email, EmailVerificationOtp.OtpType type) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.example.userservice.domain.exception.UserNotFoundException(email));

        if (type == EmailVerificationOtp.OtpType.SIGNUP) {
            if (user.isEmailVerified()) {
                return; // idempotent: already verified, don't resend
            }
            emailOtpService.generateForSignup(user);
            return;
        }

        // CHANGE_EMAIL
        if (user.getPendingEmail() == null || user.getPendingEmail().isBlank()) {
            // nothing pending -> nothing to resend
            return;
        }
        emailOtpService.generateForChangeEmail(user, user.getPendingEmail());
    }
}

