package com.example.userservice.infrastructure.security;

import com.example.userservice.domain.entities.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class PasswordResetTokenProvider {

    @Value("${app.jwt.base64-secretkey}")
    private String baseSecretKey;

    private static final long EXPIRATION_MINUTES = 15;
    private static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    /**
     * Creates a dynamic secret key combining the base secret and the user's current hashed password.
     * This ensures the token becomes invalid immediately after the password is changed.
     */
    private SecretKey getDynamicSecretKey(User user) {
        try {
            String combinedKeyString = baseSecretKey + user.getPassword();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combinedKeyString.getBytes(StandardCharsets.UTF_8));
            
            // HMAC-SHA512 needs at least a 256-bit key, which SHA-256 provides (32 bytes).
            // For HS512 it's best to have a 512-bit key, but Nimbus accepts smaller keys as long as it meets minimums.
            // Wait, HS512 requires a 512 bit (64 byte) key in strictly compliant mode.
            // Let's use SHA-512 for the digest to be safe.
            MessageDigest digest512 = MessageDigest.getInstance("SHA-512");
            byte[] hash512 = digest512.digest(combinedKeyString.getBytes(StandardCharsets.UTF_8));

            return new SecretKeySpec(hash512, "HmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not hash key", e);
        }
    }

    public String generateResetToken(User user) {
        SecretKey secretKey = getDynamicSecretKey(user);
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));

        Instant now = Instant.now();
        Instant validity = now.plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(user.getUsername())
                .claim("email", user.getEmail())
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public Jwt validateAndDecodeResetToken(String token, User user) {
        SecretKey secretKey = getDynamicSecretKey(user);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(JWT_ALGORITHM).build();
        return decoder.decode(token);
    }
}
