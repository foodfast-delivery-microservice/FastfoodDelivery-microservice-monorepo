package com.example.demo.interfaces.rest.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class LoginResponse {
    private String userId;
    private String username;
    private String accessToken;
    private String refreshToken;
}
