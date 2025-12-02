package com.example.demo.interfaces.rest.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String email;
    @NotBlank
    private String role; // admin or user
    private Boolean approved;

    // Common Profile Fields
    private String fullName;
    private String phone;
    private String address;
    private String avatar;

    // Merchant Profile Fields
    private String restaurantName;
    private String restaurantAddress;
    private String restaurantImage;
    private String openingHours;
}
