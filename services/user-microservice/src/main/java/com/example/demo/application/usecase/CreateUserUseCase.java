package com.example.demo.application.usecase;

import com.example.demo.domain.exception.EmailAlreadyExistException;
import com.example.demo.domain.exception.InvalidRoleException;
import com.example.demo.domain.exception.UsernameAlreadyExistException;
import com.example.demo.domain.model.Restaurant;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.RestaurantRepository;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.interfaces.rest.dto.user.CreateUserRequest;
import com.example.demo.interfaces.rest.dto.user.CreateUserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

public class CreateUserUseCase {
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserUseCase(UserRepository userRepository, RestaurantRepository restaurantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
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

        // Automatically create restaurant for MERCHANT users
        if (role == User.UserRole.MERCHANT && saved.getRestaurantName() != null && !saved.getRestaurantName().trim().isEmpty()) {
            // Check if restaurant already exists for this merchant
            if (!restaurantRepository.findByMerchantId(saved.getId()).isPresent()) {
                Restaurant restaurant = Restaurant.builder()
                        .merchantId(saved.getId())
                        .name(saved.getRestaurantName() != null && !saved.getRestaurantName().trim().isEmpty()
                                ? saved.getRestaurantName()
                                : "Merchant #" + saved.getId())
                        .description(null)
                        .address(saved.getRestaurantAddress() != null && !saved.getRestaurantAddress().trim().isEmpty()
                                ? saved.getRestaurantAddress()
                                : saved.getAddress() != null ? saved.getAddress() : "")
                        .city(null)
                        .district(null)
                        .image(saved.getRestaurantImage())
                        .phone(saved.getPhone())
                        .email(saved.getEmail())
                        .openingHours(saved.getOpeningHours())
                        .active(saved.isActive())
                        .approved(saved.isApproved())
                        .category(null)
                        .build();
                restaurantRepository.save(restaurant);
            }
        }

        return CreateUserResponse.fromEntity(saved);
    }
}
