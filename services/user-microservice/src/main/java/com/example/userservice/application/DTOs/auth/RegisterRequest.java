package com.example.userservice.application.DTOs.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

  @NotBlank
  private String username;
  @NotBlank
  private String email;
  @NotBlank
  private String password;
  // maybe dư
  private String role;

  // for seeding admin via api
  private String adminSecret;


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
