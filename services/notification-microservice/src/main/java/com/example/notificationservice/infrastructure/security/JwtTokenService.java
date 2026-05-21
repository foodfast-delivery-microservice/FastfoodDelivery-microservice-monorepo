package com.example.notificationservice.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@Slf4j
public class JwtTokenService {

    private static final String[] USER_ID_CLAIM_KEYS = {"user_id", "userId", "uid"};
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("(\\d+)");

    public Long extractUserId(Jwt jwt) {
        if (jwt == null) {
            log.error("JWT token is null");
            throw new IllegalArgumentException("JWT token không hợp lệ: token null");
        }

        log.debug("Extracting userId from JWT token");

        for (String claimKey : USER_ID_CLAIM_KEYS) {
            Object claimValue = jwt.getClaims().get(claimKey);
            if (claimValue != null) {
                Long userId = convertToLong(claimValue);
                if (userId != null) {
                    log.debug("✓ Resolved userId {} from JWT claim '{}'", userId, claimKey);
                    return userId;
                } else {
                    log.warn("Claim '{}' exists but cannot be converted to Long: {}", claimKey, claimValue);
                }
            }
        }

        String subject = jwt.getSubject();
        log.error("Cannot extract userId from JWT token. Available claims: {}", jwt.getClaims().keySet());
        log.error("Subject: {} (Note: Subject is username, not userId)", subject);
        
        throw new IllegalArgumentException(
                "JWT token không chứa userId claim. " +
                "Token cần có một trong các claims: " + String.join(", ", USER_ID_CLAIM_KEYS) + ". " +
                "Subject (" + subject + ") là username, không phải userId."
        );
    }

    private Long convertToLong(Object claimValue) {
        if (claimValue == null) {
            return null;
        }

        if (claimValue instanceof Number number) {
            long longValue = number.longValue();
            if (longValue > 0) {
                return longValue;
            } else {
                log.warn("Claim value is not positive: {}", longValue);
                return null;
            }
        }

        if (claimValue instanceof String stringValue) {
            String trimmed = stringValue.trim();
            
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
