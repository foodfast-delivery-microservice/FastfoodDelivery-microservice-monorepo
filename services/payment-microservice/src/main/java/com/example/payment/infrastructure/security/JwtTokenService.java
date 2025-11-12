package com.example.payment.infrastructure.security;

import com.example.payment.domain.exception.InvalidJwtTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for parsing and extracting information from JWT tokens
 */
@Service
@Slf4j
public class JwtTokenService {

    private static final String[] USER_ID_CLAIM_KEYS = {"user_id", "userId", "uid"};
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("(\\d+)");

    /**
     * Extract userId from JWT token
     * Tries to find userId in claims (user_id, userId, uid) or in subject
     *
     * @param jwt JWT token
     * @return userId extracted from token
     * @throws InvalidJwtTokenException if token is null or userId cannot be extracted
     */
    public Long extractUserId(Jwt jwt) {
        if (jwt == null) {
            log.error("JWT token is null");
            throw new InvalidJwtTokenException("JWT token không hợp lệ: token null");
        }

        log.debug("Extracting userId from JWT token");

        // First, try to find userId in claims (user_id, userId, uid)
        for (String claimKey : USER_ID_CLAIM_KEYS) {
            Object claimValue = jwt.getClaims().get(claimKey);
            if (claimValue != null) {
                Long userId = convertToLong(claimValue);
                if (userId != null) {
                    log.info("✓ Resolved userId {} from JWT claim '{}'", userId, claimKey);
                    return userId;
                } else {
                    log.warn("Claim '{}' exists but cannot be converted to Long: {}", claimKey, claimValue);
                }
            }
        }

        // Log all available claims for debugging
        String subject = jwt.getSubject();
        log.error("Cannot extract userId from JWT token. Available claims: {}", jwt.getClaims().keySet());
        log.error("Subject: {} (Note: Subject is username, not userId)", subject);
        log.error("Expected claims: {}", String.join(", ", USER_ID_CLAIM_KEYS));
        
        // KHÔNG parse userId từ subject vì subject là username, không phải userId
        // Ví dụ: subject="user102" nhưng userId thực sự có thể là 3
        throw new InvalidJwtTokenException(
                "JWT token không chứa userId claim. " +
                "Token cần có một trong các claims: " + String.join(", ", USER_ID_CLAIM_KEYS) + ". " +
                "Subject (" + subject + ") là username, không phải userId."
        );
    }

    /**
     * Validate JWT token format and required claims
     *
     * @param jwt JWT token to validate
     * @return true if token is valid
     * @throws InvalidJwtTokenException if token is invalid
     */
    public boolean validateToken(Jwt jwt) {
        if (jwt == null) {
            log.error("JWT token is null");
            throw new InvalidJwtTokenException("JWT token không hợp lệ: token null");
        }

        // Check if token has required structure
        if (jwt.getClaims() == null || jwt.getClaims().isEmpty()) {
            log.error("JWT token has no claims");
            throw new InvalidJwtTokenException("JWT token không hợp lệ: không có claims");
        }

        // Try to extract userId to validate token is usable
        try {
            extractUserId(jwt);
            log.debug("✓ JWT token validated successfully");
            return true;
        } catch (InvalidJwtTokenException e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get raw token value as String
     *
     * @param jwt JWT token
     * @return raw token value
     * @throws InvalidJwtTokenException if token is null
     */
    public String getTokenValue(Jwt jwt) {
        if (jwt == null) {
            log.error("JWT token is null");
            throw new InvalidJwtTokenException("JWT token không hợp lệ: token null");
        }

        return jwt.getTokenValue();
    }

    /**
     * Convert claim value to Long
     * Handles Number, String (numeric), and String with numeric parts
     *
     * @param claimValue claim value to convert
     * @return Long value or null if cannot convert
     */
    private Long convertToLong(Object claimValue) {
        if (claimValue == null) {
            return null;
        }

        // Handle Number types (Integer, Long, etc.)
        if (claimValue instanceof Number number) {
            long longValue = number.longValue();
            if (longValue > 0) {
                return longValue;
            } else {
                log.warn("Claim value is not positive: {}", longValue);
                return null;
            }
        }

        // Handle String types
        if (claimValue instanceof String stringValue) {
            String trimmed = stringValue.trim();
            
            // If string is purely numeric, parse directly
            if (trimmed.matches("\\d+")) {
                try {
                    long longValue = Long.parseLong(trimmed);
                    if (longValue > 0) {
                        return longValue;
                    } else {
                        log.warn("Parsed value is not positive: {}", longValue);
                        return null;
                    }
                } catch (NumberFormatException ex) {
                    log.warn("Unable to parse numeric user id from JWT string claim '{}'", stringValue, ex);
                    return null;
                }
            } else {
                // Attempt to extract numeric part from string (e.g., "user_123" -> 123)
                java.util.regex.Matcher matcher = NUMERIC_PATTERN.matcher(trimmed);
                if (matcher.find()) {
                    String numericPart = matcher.group(1);
                    try {
                        long longValue = Long.parseLong(numericPart);
                        if (longValue > 0) {
                            log.debug("Extracted numeric part '{}' from JWT claim '{}'", numericPart, stringValue);
                            return longValue;
                        } else {
                            log.warn("Extracted numeric part is not positive: {}", longValue);
                            return null;
                        }
                    } catch (NumberFormatException ex) {
                        log.warn("Unable to parse extracted numeric part '{}' from JWT claim '{}'", 
                                numericPart, stringValue, ex);
                        return null;
                    }
                } else {
                    log.debug("JWT string claim '{}' does not contain any numeric part, ignoring", stringValue);
                    return null;
                }
            }
        }

        log.warn("Unsupported claim value type: {}", claimValue.getClass().getName());
        return null;
    }
}

