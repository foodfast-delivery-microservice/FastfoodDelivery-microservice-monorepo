package com.example.demo.interfaces.rest.dto.user;

import lombok.Data;

@Data
public class UserPatchDTO {
    private String username;
    private String email;
    private String role;
    private Boolean approved;
    private Boolean active;

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
