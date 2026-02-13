package com.example.userservice.domain.port;

/**
 * Domain port for password encoding operations.
 * This interface belongs to the domain layer and has no framework dependencies.
 * Implementations are provided in the infrastructure layer.
 */
public interface PasswordEncoderPort {
    
    /**
     * Encode (hash) a raw password.
     * 
     * @param rawPassword The raw password to encode
     * @return The encoded password
     */
    String encode(String rawPassword);
    
    /**
     * Verify if a raw password matches an encoded password.
     * 
     * @param rawPassword The raw password to verify
     * @param encodedPassword The encoded password to compare against
     * @return true if the raw password matches the encoded password
     */
    boolean matches(String rawPassword, String encodedPassword);
}
