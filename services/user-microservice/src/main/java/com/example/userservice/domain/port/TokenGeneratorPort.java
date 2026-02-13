package com.example.userservice.domain.port;

import com.example.userservice.domain.entities.User;

/**
 * Domain port for token generation operations.
 * This interface belongs to the domain layer and has no framework dependencies.
 * Implementations are provided in the infrastructure layer.
 */
public interface TokenGeneratorPort {
    
    /**
     * Generate an access token for a user.
     * 
     * @param user The user to generate token for
     * @return The access token string
     */
    String createAccessToken(User user);
    
    /**
     * Generate a refresh token for a username.
     * 
     * @param username The username to generate token for
     * @return The refresh token string
     */
    String createRefreshToken(String username);
}
