package com.example.demo.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
public class jwtConfig {

    @Value("${app.jwt.base64-secretkey}")
    private String jwtKey;

    @Bean
    public JwtEncoder jwtEncoder() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, MacAlgorithm.HS512.getName());
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }
}