package com.example.demo.application.usecase;

import com.example.demo.domain.exception.EmailAlreadyExistException;
import com.example.demo.domain.exception.InvalidRoleException;
import com.example.demo.domain.exception.UsernameAlreadyExistException;
import com.example.demo.domain.model.Restaurant;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.RestaurantRepository;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.infrastructure.service.GeocodingService;
import com.example.demo.interfaces.rest.dto.user.CreateUserRequest;
import com.example.demo.interfaces.rest.dto.user.CreateUserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

public class CreateUserUseCase {
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeocodingService geocodingService;

    public CreateUserUseCase(UserRepository userRepository, RestaurantRepository restaurantRepository, PasswordEncoder passwordEncoder, GeocodingService geocodingService) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.passwordEncoder = passwordEncoder;
        this.geocodingService = geocodingService;
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
                Restaurant.RestaurantBuilder restaurantBuilder = Restaurant.builder()
                        .merchantId(saved.getId())
                        .name(saved.getRestaurantName() != null && !saved.getRestaurantName().trim().isEmpty()
                                ? saved.getRestaurantName()
                                : "Merchant #" + saved.getId())
                        .description(createUserRequest.getRestaurantDescription())
                        .address(saved.getRestaurantAddress() != null && !saved.getRestaurantAddress().trim().isEmpty()
                                ? saved.getRestaurantAddress()
                                : saved.getAddress() != null ? saved.getAddress() : "")
                        .city(createUserRequest.getRestaurantCity())
                        .district(createUserRequest.getRestaurantDistrict())
                        .image(saved.getRestaurantImage())
                        .phone(saved.getPhone())
                        .email(saved.getEmail())
                        .openingHours(saved.getOpeningHours())
                        .active(saved.isActive())
                        .approved(saved.isApproved())
                        .category(createUserRequest.getRestaurantCategory());
                
                // Set coordinates if provided from frontend, otherwise geocode from address
                if (createUserRequest.getRestaurantLatitude() != null && createUserRequest.getRestaurantLongitude() != null) {
                    restaurantBuilder.latitude(BigDecimal.valueOf(createUserRequest.getRestaurantLatitude()));
                    restaurantBuilder.longitude(BigDecimal.valueOf(createUserRequest.getRestaurantLongitude()));
                } else {
                    // Try to geocode address if coordinates not provided
                    String addressToGeocode = saved.getRestaurantAddress() != null && !saved.getRestaurantAddress().trim().isEmpty()
                            ? saved.getRestaurantAddress()
                            : saved.getAddress() != null ? saved.getAddress() : null;
                    
                    if (addressToGeocode != null) {
                        geocodingService.geocode(addressToGeocode).ifPresent(geoResult -> {
                            restaurantBuilder.latitude(geoResult.getLat());
                            restaurantBuilder.longitude(geoResult.getLon());
                        });
                    }
                }
                
                // Set delivery fee if provided
                if (createUserRequest.getRestaurantDeliveryFee() != null) {
                    restaurantBuilder.deliveryFee(createUserRequest.getRestaurantDeliveryFee());
                }
                
                // Set estimated delivery time if provided
                if (createUserRequest.getRestaurantEstimatedDeliveryTime() != null) {
                    restaurantBuilder.estimatedDeliveryTime(createUserRequest.getRestaurantEstimatedDeliveryTime());
                }
                
                Restaurant restaurant = restaurantBuilder.build();
                restaurantRepository.save(restaurant);
            }
        }

        return CreateUserResponse.fromEntity(saved);
    }
}
