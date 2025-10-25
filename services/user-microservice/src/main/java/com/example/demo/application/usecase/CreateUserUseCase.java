package com.example.demo.application.usecase;

import com.example.demo.domain.exception.EmailAlreadyExistException;
import com.example.demo.domain.exception.InvalidRoleException;
import com.example.demo.domain.exception.UsernameAlreadyExistException;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.interfaces.rest.dto.user.CreateUserRequest;
import com.example.demo.interfaces.rest.dto.user.CreateUserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

public class  CreateUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public CreateUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // cho user tự đăng kí
    public CreateUserResponse execute(CreateUserRequest createUserRequest) {
        if (userRepository.existsByEmail(createUserRequest.getEmail())) {
            throw  new EmailAlreadyExistException(createUserRequest.getEmail());
        }
        if (userRepository.existsByUsername(createUserRequest.getUsername())) {
            throw  new UsernameAlreadyExistException(createUserRequest.getUsername());
        }
        User.UserRole role;
        try {
            role = createUserRequest.getRole() == null
                    ? User.UserRole.USER
                    : User.UserRole.valueOf(createUserRequest.getRole());
        }catch (IllegalArgumentException e){
            throw new InvalidRoleException(createUserRequest.getRole());
        }

        User user = new User();
        user.setUsername(createUserRequest.getUsername());
        user.setEmail(createUserRequest.getEmail());
        user.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));
        user.setRole(role);

        User saved =  userRepository.save(user);

        return new CreateUserResponse(saved.getId(),saved.getUsername(),saved.getEmail(),saved.getRole().name());
    }
}
