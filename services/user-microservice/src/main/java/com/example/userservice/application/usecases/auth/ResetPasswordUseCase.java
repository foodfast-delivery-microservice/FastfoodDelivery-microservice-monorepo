package com.example.userservice.application.usecases.auth;

import com.example.userservice.application.DTOs.auth.ResetPasswordRequest;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.exception.UserNotFoundException;
import com.example.userservice.domain.port.PasswordEncoderPort;
import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.infrastructure.security.PasswordResetTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResetPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final PasswordResetTokenProvider passwordResetTokenProvider;

    @Transactional
    public void execute(ResetPasswordRequest request) {
        // Since we need the user's old password to decode the token, we first need to extract the email/username.
        // But we can't safely decode without the key.
        // Wait! We can parse the JWT without verifying the signature to get the claims, then verify it.
        // However, NimbusJwtDecoder verifies it automatically.
        // Let's implement a safe way to extract claims without verification, or we can use Nimbus JWT Parser.
        // Actually, for simplicity and security, it's better to pass the email in the request if we can't decode it first.
        // Let's use Nimbus to parse unverified to get the subject (username).
        
        try {
            com.nimbusds.jwt.JWT parsedJwt = com.nimbusds.jwt.JWTParser.parse(request.getToken());
            String username = parsedJwt.getJWTClaimsSet().getSubject();
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException(username));

            // Now validate and decode WITH signature verification
            Jwt jwt = passwordResetTokenProvider.validateAndDecodeResetToken(request.getToken(), user);
            
            // If we reached here, the token is valid, not expired, and the signature matches (meaning password hasn't changed).
            String encodedNewPassword = passwordEncoderPort.encode(request.getNewPassword());
            user.setPassword(encodedNewPassword);
            userRepository.save(user);
            
            log.info("Password reset successfully for user: {}", username);
            
        } catch (java.text.ParseException e) {
            log.error("Invalid token format", e);
            throw new IllegalArgumentException("Token không hợp lệ.");
        } catch (org.springframework.security.oauth2.jwt.JwtException e) {
            log.error("Token verification failed", e);
            throw new IllegalArgumentException("Token đã hết hạn hoặc không hợp lệ (có thể bạn đã đổi mật khẩu rồi).");
        }
    }
}
