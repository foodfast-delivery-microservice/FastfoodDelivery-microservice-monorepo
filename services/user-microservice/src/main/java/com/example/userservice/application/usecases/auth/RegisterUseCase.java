package com.example.userservice.application.usecases.auth;

import com.example.userservice.domain.exception.DisposableEmailNotAllowedException;
import com.example.userservice.domain.exception.EmailAlreadyExistException;
import com.example.userservice.domain.exception.InvalidRoleException;
import com.example.userservice.domain.exception.UsernameAlreadyExistException;
import com.example.userservice.domain.entities.Restaurant;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.port.PasswordEncoderPort;
import com.example.userservice.domain.repository.RestaurantRepository;
import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.application.DTOs.auth.RegisterRequest;
import com.example.userservice.application.DTOs.user.CreateUserResponse;
import com.example.userservice.domain.exception.InvalidEmailDomainException;
import com.example.userservice.application.service.EmailDomainValidator;

public class RegisterUseCase {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final com.example.userservice.domain.repository.OutboxEventRepository outboxEventRepository;
    private final com.example.userservice.application.service.EventPayloadSerializer eventPayloadSerializer;
    private final com.example.userservice.application.service.EmailOtpService emailOtpService;
    private final com.example.userservice.application.service.DisposableEmailValidator disposableEmailValidator;
    private final EmailDomainValidator emailDomainValidator;

    private final String adminSecretKey;

            com.example.userservice.application.service.DisposableEmailValidator disposableEmailValidator,
            EmailDomainValidator emailDomainValidator,
            String adminSecretKey) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.passwordEncoderPort = passwordEncoderPort;
        this.outboxEventRepository = outboxEventRepository;
        this.eventPayloadSerializer = eventPayloadSerializer;
        this.emailOtpService = emailOtpService;
        this.disposableEmailValidator = disposableEmailValidator;
        this.emailDomainValidator = emailDomainValidator;
        this.adminSecretKey = adminSecretKey;
    }

    // cho user tự đăng kí
    public CreateUserResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UsernameAlreadyExistException(registerRequest.getUsername());
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyExistException(registerRequest.getEmail());
        }
        if (disposableEmailValidator.isDisposable(registerRequest.getEmail())) {
            throw new DisposableEmailNotAllowedException(registerRequest.getEmail());
        }
        if (!emailDomainValidator.isValidDomain(registerRequest.getEmail())) {
            throw new InvalidEmailDomainException(registerRequest.getEmail());
        }

        User.UserRole role = resolveRole(registerRequest.getRole(), registerRequest.getAdminSecret());
        boolean approved = role != User.UserRole.MERCHANT;

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoderPort.encode(registerRequest.getPassword()));
        user.setRole(role);
        user.setApproved(approved);
        user.setActive(true);
        user.setEmailVerified(true); // Auto-verify email temporarily

        // Map Profile Fields
        user.setFullName(registerRequest.getFullName());
        user.setPhone(registerRequest.getPhone());
        user.setAddress(registerRequest.getAddress());
        user.setAvatar(registerRequest.getAvatar());

        // Map Merchant Fields
        user.setRestaurantName(registerRequest.getRestaurantName());
        user.setRestaurantAddress(registerRequest.getRestaurantAddress());
        user.setRestaurantImage(registerRequest.getRestaurantImage());
        user.setOpeningHours(registerRequest.getOpeningHours());

        User saved = userRepository.save(user);

        // -- CREATE OUTBOX EVENT FOR RELIABLE EVENT PUBLISHING --
        createUserRegisteredOutboxEvent(saved);
        // emailOtpService.generateForSignup(saved); // Temporarily disabled OTP generation

        // Automatically create restaurant for MERCHANT users
        if (role == User.UserRole.MERCHANT && saved.getRestaurantName() != null
                && !saved.getRestaurantName().trim().isEmpty()) {
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

    private User.UserRole resolveRole(String requestedRole, String providedAdminSecret) {
        if (requestedRole == null || requestedRole.trim().isEmpty()) {
            return User.UserRole.USER;
        }

        try {
            User.UserRole role = User.UserRole.valueOf(requestedRole.trim().toUpperCase());
            if (role == User.UserRole.ADMIN) {
                if (providedAdminSecret == null || !providedAdminSecret.equals(this.adminSecretKey)) {
                    throw new InvalidRoleException(requestedRole);
                }
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new InvalidRoleException(requestedRole);
        }
    }

    private void createUserRegisteredOutboxEvent(User user) {
        com.example.userservice.application.DTOs.event.UserRegisteredEvent eventDTO = com.example.userservice.application.DTOs.event.UserRegisteredEvent
                .builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .registeredAt(java.time.LocalDateTime.now())
                .build();

        String payloadJson = eventPayloadSerializer.serialize(eventDTO);

        com.example.userservice.domain.entities.OutboxEvent event = com.example.userservice.domain.entities.OutboxEvent
                .builder()
                .aggregateType("User")
                .aggregateId(user.getId().toString())
                .type("UserRegistered")
                .payload(payloadJson)
                .status(com.example.userservice.domain.valueobjects.EventStatus.NEW)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        outboxEventRepository.save(event);
    }
}
