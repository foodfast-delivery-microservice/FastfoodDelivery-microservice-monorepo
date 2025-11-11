package com.example.order_service.domain.repository;

import com.example.order_service.application.dto.UserValidationResponse;

/**
 * Port interface for User Service integration
 * Follows the same pattern as ProductServicePort
 */
public interface UserServicePort {
    /**
     * Validate user exists and is active
     * @param userId User ID to validate
     * @return UserValidationResponse with validation result
     */
    UserValidationResponse validateUser(Long userId);
}

