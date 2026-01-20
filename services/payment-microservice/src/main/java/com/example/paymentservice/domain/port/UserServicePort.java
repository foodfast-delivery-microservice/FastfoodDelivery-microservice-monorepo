package com.example.paymentservice.domain.port;

import com.example.paymentservice.application.dto.UserValidationResponse;

/**
 * Port interface for User Service integration
 * Used to validate user exists
 */
public interface UserServicePort {
    /**
     * Validate user exists
     * @param userId User ID to validate
     * @return UserValidationResponse with validation result
     */
    UserValidationResponse validateUser(Long userId);
}

