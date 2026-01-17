package com.example.userservice.application.usecases.user;

import com.example.userservice.domain.exception.InvalidId;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.application.DTOs.user.CreateUserResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

public class GetUserByIdUseCase {
    private final UserRepository userRepository;

    public GetUserByIdUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CreateUserResponse execute(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new InvalidId(id));
        return CreateUserResponse.fromEntity(user);
    }
}
