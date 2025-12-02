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
    
    // Restaurant Additional Fields
    private String restaurantDescription;
    private String restaurantCity;
    private String restaurantDistrict;
    private String restaurantCategory;
    private Double restaurantLatitude;
    private Double restaurantLongitude;
    private java.math.BigDecimal restaurantDeliveryFee;
    private Integer restaurantEstimatedDeliveryTime;
}
