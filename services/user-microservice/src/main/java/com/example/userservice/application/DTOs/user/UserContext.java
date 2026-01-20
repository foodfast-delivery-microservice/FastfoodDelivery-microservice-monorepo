package com.example.userservice.application.DTOs.user;

import java.util.Set;

/**
 * DTO representing user context information extracted from authentication.
 * This DTO is used to pass user information to use cases without coupling
 * them to Spring Security framework.
 */
public record UserContext(
        String username,
        Long userId,
        Set<String> roles,
        boolean isAdmin
) {
    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return roles.contains(role) || roles.contains("ROLE_" + role);
    }
}
