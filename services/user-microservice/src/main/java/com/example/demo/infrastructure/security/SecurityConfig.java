package com.example.demo.infrastructure.security;

import org.springframework.http.HttpMethod;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Actuator endpoints for health checks (required by Eureka)
                        .requestMatchers("/actuator/**").permitAll()
                        
                        // Internal API for service-to-service calls (no authentication required)
                        .requestMatchers("/api/internal/**").permitAll()

                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/api/v1/restaurants/me/**").hasAnyRole("MERCHANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/restaurants/me").hasAnyRole("MERCHANT", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/restaurants/me/**").hasAnyRole("MERCHANT", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/restaurants/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/restaurants/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/restaurants/admin/all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/restaurants/**").permitAll()
                        // Validation endpoint cho phép USER role (cho Order Service)
                        // Pattern: /api/v1/users/{id}/validate - chỉ match 1 level path variable
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/validate")
                        .hasAnyRole("USER", "ADMIN", "MERCHANT")

                        // Allow getting own profile
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()

                        // Allow getting specific user/restaurant details (public)
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/{id:[0-9]+}").permitAll()

                        // Các endpoint khác yêu cầu ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN")

                        .anyRequest().authenticated())

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
