package com.example.gatewayservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtTokenForwardFilter jwtTokenForwardFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        public SecurityConfig(JwtTokenForwardFilter jwtTokenForwardFilter,
                        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
                this.jwtTokenForwardFilter = jwtTokenForwardFilter;
                this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        }

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http, JwtAuthConverter jwtAuthConverter) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                                                // USER endpoints - specific patterns FIRST
                                                // Validation endpoint for inter-service calls
                                                .requestMatchers(HttpMethod.GET, "/api/v1/users/*/validate")
                                                .hasAnyRole("USER", "ADMIN", "MERCHANT")

                                                // PATCH endpoints - only need authentication (authorization in UseCase)
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/users/**").authenticated()

                                                // Other USER endpoints require ADMIN
                                                .requestMatchers(HttpMethod.POST, "/api/v1/users/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN")

                                                // PRODUCT endpoints - specific patterns first
                                                .requestMatchers("/api/v1/products/merchants/**")
                                                .hasAnyRole("MERCHANT", "ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/v1/products/**")
                                                .hasAnyRole("ADMIN", "MERCHANT")
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**")
                                                .hasAnyRole("ADMIN", "MERCHANT")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**")
                                                .hasAnyRole("ADMIN", "MERCHANT")

                                                // ORDER & PAYMENT endpoints - specific patterns first
                                                .requestMatchers("/api/v1/orders/merchants/**")
                                                .hasAnyRole("MERCHANT", "ADMIN")
                                                .requestMatchers("/api/v1/orders/**").authenticated()
                                                .requestMatchers("/api/v1/payments/merchants/**")
                                                .hasAnyRole("MERCHANT", "ADMIN")
                                                .requestMatchers("/api/v1/payments/**").authenticated()

                                                .anyRequest().authenticated())
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                                .addFilterAfter(jwtTokenForwardFilter, UsernamePasswordAuthenticationFilter.class)

                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));

                return http.build();
        }
}