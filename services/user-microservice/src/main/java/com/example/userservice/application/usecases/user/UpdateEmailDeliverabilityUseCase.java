package com.example.userservice.application.usecases.user;

import com.example.userservice.application.DTOs.user.UpdateEmailDeliverabilityRequest;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.exception.InvalidId;
import com.example.userservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateEmailDeliverabilityUseCase {

    private final UserRepository userRepository;

    public User execute(Long userId, UpdateEmailDeliverabilityRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidId(userId));

        user.setEmailUndeliverable(Boolean.TRUE.equals(request.getUndeliverable()));

        if (request.getBouncedAt() != null) {
            user.setLastBounceAt(request.getBouncedAt());
        }

        int currentBounceCount = user.getBounceCount();
        int increment = request.getBounceIncrement() == null ? 0 : Math.max(0, request.getBounceIncrement());
        user.setBounceCount(currentBounceCount + increment);

        return userRepository.save(user);
    }
}
