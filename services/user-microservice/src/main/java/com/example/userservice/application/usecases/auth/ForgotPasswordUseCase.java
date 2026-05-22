package com.example.userservice.application.usecases.auth;

import com.example.userservice.application.DTOs.auth.ForgotPasswordRequest;
import com.example.userservice.application.DTOs.event.UserForgotPasswordEvent;
import com.example.userservice.domain.entities.OutboxEvent;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.exception.UserNotFoundException;
import com.example.userservice.domain.repository.OutboxEventRepository;
import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.infrastructure.security.PasswordResetTokenProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ForgotPasswordUseCase {
    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordResetTokenProvider passwordResetTokenProvider;

    @Transactional
    public void execute(ForgotPasswordRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isEmpty()) {
            // For security reasons, we do not throw an exception here.
            // We just log and return so attackers cannot enumerate valid emails.
            log.warn("Forgot password requested for non-existent email: {}", request.getEmail());
            return;
        }

        User user = optionalUser.get();
        String resetToken = passwordResetTokenProvider.generateResetToken(user);

        // Create Outbox Event
        UserForgotPasswordEvent eventDTO = UserForgotPasswordEvent.builder()
                .email(user.getEmail())
                .resetToken(resetToken)
                .requestedAt(LocalDateTime.now())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            String payload = mapper.writeValueAsString(eventDTO);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(user.getId().toString())
                    .aggregateType("User")
                    .type("UserForgotPasswordEvent")
                    .payload(payload)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Saved UserForgotPasswordEvent to outbox for email: {}", user.getEmail());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize UserForgotPasswordEvent", e);
            throw new RuntimeException("Failed to serialize UserForgotPasswordEvent", e);
        }
    }
}
