package com.example.demo.application.usecase;

import com.example.demo.domain.repository.UserRepository;
import com.example.demo.interfaces.rest.dto.user.CreateUserResponse;

import java.util.List;
import java.util.stream.Collectors;

public class GetAllUsersUseCase {
    private final UserRepository userRepository;

    public GetAllUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public List<CreateUserResponse> execute(){
        return userRepository.findAll()
                .stream()
                .map(CreateUserResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
