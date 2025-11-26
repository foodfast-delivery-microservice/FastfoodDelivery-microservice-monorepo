package com.example.demo.application.usecase;

import com.example.demo.domain.exception.EmailAlreadyExistException;
import com.example.demo.domain.exception.InvalidRoleException;
import com.example.demo.domain.exception.UsernameAlreadyExistException;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.interfaces.rest.dto.auth.RegisterRequest;
import com.example.demo.interfaces.rest.dto.user.CreateUserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

public class RegisterUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    // cho user tự đăng kí
    public CreateUserResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UsernameAlreadyExistException(registerRequest.getUsername());
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw  new EmailAlreadyExistException(registerRequest.getEmail());
        }

        User.UserRole role = resolveRole(registerRequest.getRole());
        boolean approved = role != User.UserRole.MERCHANT;

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(role);
        user.setApproved(approved);
        user.setActive(true);

        User saved = userRepository.save(user);
        return new CreateUserResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getRole().name(),
                saved.isApproved(),
                saved.isActive()
        );
    }

    private User.UserRole resolveRole(String requestedRole) {
        if (requestedRole == null || requestedRole.trim().isEmpty()) {
            return User.UserRole.USER;
        }

        try {
            User.UserRole role = User.UserRole.valueOf(requestedRole.trim().toUpperCase());
            if (role == User.UserRole.ADMIN) {
                throw new InvalidRoleException(requestedRole);
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new InvalidRoleException(requestedRole);
        }
    }
}
