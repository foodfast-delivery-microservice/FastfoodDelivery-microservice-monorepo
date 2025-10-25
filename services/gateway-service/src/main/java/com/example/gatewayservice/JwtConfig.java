package com.example.gatewayservice;

import com.nimbusds.jose.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {
    @Value("${app.jwt.base64-secretkey}")
    private String jwtKey;

    @Value("${app.jwt.access.expiration-in-seconds}")
    private long jwtAccessExpiration;

    @Value("${app.jwt.refresh.expiration-in-seconds}")
    private long jwtRefreshExpiration;

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(getSecretKey())
                .macAlgorithm(JWT_ALGORITHM)
                .build();

        return token -> {
            try {
                return jwtDecoder.decode(token);
            } catch (Exception e) {
                System.out.println("JWT error: " + e.getMessage());
                throw new RuntimeException(); // đó ném lỗi sau
            }
        };
    }
    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    @Bean
    JwtAuthConverter jwtAuthConverter() {
        return new JwtAuthConverter("role");
    }
}
