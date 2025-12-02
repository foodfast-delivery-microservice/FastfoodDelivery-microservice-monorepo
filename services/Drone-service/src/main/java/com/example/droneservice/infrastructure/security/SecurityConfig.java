package com.example.droneservice.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filter(
            HttpSecurity http,
            JwtAuthConverter jwtAuthConverter,
            @Value("${app.security.enabled:true}") boolean securityEnabled) throws Exception {
        http.csrf(csrf -> csrf.disable());

        if (!securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/actuator/**").permitAll()

                        // Admin-only endpoints (Drone management)
                        .requestMatchers(HttpMethod.POST, "/api/drones").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/drones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/drones/**").hasRole("ADMIN")

                        // Admin can view all drones and missions
                        .requestMatchers(HttpMethod.GET, "/api/drones/**").hasAnyRole("ADMIN", "SERVICE")
                        .requestMatchers(HttpMethod.GET, "/api/missions").hasAnyRole("ADMIN", "SERVICE")

                        // Users can track their own orders
                        .requestMatchers(HttpMethod.GET, "/api/missions/order/*/tracking").authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    JwtAuthConverter jwtAuthConverter(JwtGrantedAuthoritiesConverter grantedConverter) {
        return new JwtAuthConverter(grantedConverter);
    }

    @Bean
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter() {
        return new JwtGrantedAuthoritiesConverter("role");
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
    JwtDecoder jwtDecoder(@Value("${app.jwt.base64-secretkey}") String secretKey) {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(secretKey);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.from("HS512"))
                .build();
    }
}
