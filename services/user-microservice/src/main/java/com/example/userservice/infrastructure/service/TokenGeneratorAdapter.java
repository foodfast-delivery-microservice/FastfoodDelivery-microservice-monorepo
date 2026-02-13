package com.example.userservice.infrastructure.service;

import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.port.TokenGeneratorPort;
import com.example.userservice.infrastructure.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing TokenGeneratorPort using SecurityUtil.
 * This bridges the domain port with Spring Security JWT infrastructure.
 */
@Component
@RequiredArgsConstructor
public class TokenGeneratorAdapter implements TokenGeneratorPort {
    
    private final SecurityUtil securityUtil;
    
    @Override
    public String createAccessToken(User user) {
        return securityUtil.createAccessToken(user);
    }
    
    @Override
    public String createRefreshToken(String username) {
        return securityUtil.createRefreshToken(username);
    }
}
