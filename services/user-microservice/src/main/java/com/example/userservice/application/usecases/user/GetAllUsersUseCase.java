package com.example.userservice.application.usecases.user;

import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.application.DTOs.user.CreateUserResponse;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

public class GetAllUsersUseCase {
    private final UserRepository userRepository;

    public GetAllUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<CreateUserResponse> execute() {
        return userRepository.findAll()
                .stream()
                .map(CreateUserResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
