package com.example.userservice.application.usecases.auth;

import com.example.userservice.application.service.EmailOtpService;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestChangeEmailUseCase {

    private final UserRepository userRepository;
    private final EmailOtpService emailOtpService;

    @Transactional
    public void requestChangeEmail(Long userId, String newEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.example.userservice.domain.exception.InvalidId(userId));

        if (userRepository.existsByEmail(newEmail)) {
            throw new com.example.userservice.domain.exception.EmailAlreadyExistException(newEmail);
        }

        user.setPendingEmail(newEmail);
        user.setEmailVerified(false);
        userRepository.save(user);

        emailOtpService.generateForChangeEmail(user, newEmail);
    }
}

