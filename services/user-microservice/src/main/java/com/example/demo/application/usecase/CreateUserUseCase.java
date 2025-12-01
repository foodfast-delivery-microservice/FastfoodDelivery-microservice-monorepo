package com.example.demo.application.usecase;

import com.example.demo.domain.exception.EmailAlreadyExistException;
import com.example.demo.domain.exception.InvalidRoleException;
import com.example.demo.domain.exception.UsernameAlreadyExistException;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.interfaces.rest.dto.user.CreateUserRequest;
import com.example.demo.interfaces.rest.dto.user.CreateUserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

public class CreateUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // chỉ có role admin mới làm được
    public CreateUserResponse execute(CreateUserRequest createUserRequest) {
        if (userRepository.existsByEmail(createUserRequest.getEmail())) {
            throw new EmailAlreadyExistException(createUserRequest.getEmail());
        }
        if (userRepository.existsByUsername(createUserRequest.getUsername())) {
            throw new UsernameAlreadyExistException(createUserRequest.getUsername());
        }
        User.UserRole role;
        try {
            role = createUserRequest.getRole() == null
                    ? User.UserRole.USER
                    : User.UserRole.valueOf(createUserRequest.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException(createUserRequest.getRole());
        }

        // Determine approved status: use provided value, or default based on role
        // MERCHANT roles need admin approval (false), others are approved by default
        // (true)
        boolean approved = createUserRequest.getApproved() != null
                ? createUserRequest.getApproved()
                : role != User.UserRole.MERCHANT;

        User user = new User();
        user.setUsername(createUserRequest.getUsername());
        user.setEmail(createUserRequest.getEmail());
        user.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));
        user.setRole(role);
        user.setApproved(approved);
        user.setActive(true);

        // Map Profile Fields
        user.setFullName(createUserRequest.getFullName());
        user.setPhone(createUserRequest.getPhone());
        user.setAddress(createUserRequest.getAddress());
        user.setAvatar(createUserRequest.getAvatar());

        // Map Merchant Fields
        user.setRestaurantName(createUserRequest.getRestaurantName());
        user.setRestaurantAddress(createUserRequest.getRestaurantAddress());
        user.setRestaurantImage(createUserRequest.getRestaurantImage());
        user.setOpeningHours(createUserRequest.getOpeningHours());

        User saved = userRepository.save(user);

        return CreateUserResponse.fromEntity(saved);
    }
}
