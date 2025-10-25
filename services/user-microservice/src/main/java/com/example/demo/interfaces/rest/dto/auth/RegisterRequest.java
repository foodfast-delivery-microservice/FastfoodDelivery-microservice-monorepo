package com.example.demo.interfaces.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

  @NotBlank private String username;
  @NotBlank private String email;
  @NotBlank private String password;

}
