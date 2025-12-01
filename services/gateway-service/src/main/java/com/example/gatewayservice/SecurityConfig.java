package com.example.gatewayservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtTokenForwardFilter jwtTokenForwardFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final PublicEndpointFilter publicEndpointFilter;
        private final PublicEndpointBearerTokenResolver publicEndpointBearerTokenResolver;

        public SecurityConfig(JwtTokenForwardFilter jwtTokenForwardFilter,
                        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                        PublicEndpointFilter publicEndpointFilter,
                        PublicEndpointBearerTokenResolver publicEndpointBearerTokenResolver) {
                this.jwtTokenForwardFilter = jwtTokenForwardFilter;
                this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
                this.publicEndpointFilter = publicEndpointFilter;
                this.publicEndpointBearerTokenResolver = publicEndpointBearerTokenResolver;
        }

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http, JwtAuthConverter jwtAuthConverter) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                                                // Restaurant endpoints
                                                .requestMatchers("/api/v1/restaurants/me/**")
                                                .hasAnyRole("MERCHANT", "ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/restaurants/me")
                                                .hasAnyRole("MERCHANT", "ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/restaurants/me/**")
                                                .hasAnyRole("MERCHANT", "ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/v1/restaurants/**").permitAll()

                                                // USER endpoints - specific patterns FIRST
                                                // Validation endpoint for inter-service calls
                                                .requestMatchers(HttpMethod.GET, "/api/v1/users/*/validate")
                                                .hasAnyRole("USER", "ADMIN", "MERCHANT")

                                                // Allow getting own profile
                                                .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()

                                                // PATCH endpoints - only need authentication (authorization in UseCase)
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/users/**").authenticated()

                                                // Allow getting specific user/restaurant details (public)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/users/{id:[0-9]+}")
                                                .permitAll()

                                                // Other USER endpoints require ADMIN
                                                .requestMatchers(HttpMethod.POST, "/api/v1/users/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN")

                                                // PRODUCT endpoints - specific patterns first
                                                // Public endpoint: GET /products/merchants/{merchantId} - for guests
                                                .requestMatchers(HttpMethod.GET, "/api/v1/products/merchants/{merchantId:[0-9]+}")
                                                .permitAll()
                                                // Authenticated endpoint: GET /products/merchants/me - for merchant
                                                .requestMatchers("/api/v1/products/merchants/me")
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

                                                .requestMatchers("/actuator/**").permitAll()

                                                // Admin-only endpoints (Drone management)
                                                .requestMatchers(HttpMethod.POST, "/api/drones").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/drones/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/drones/**").hasRole("ADMIN")

                                                // Admin can view all drones and missions
                                                .requestMatchers(HttpMethod.GET, "/api/drones/**")
                                                .hasAnyRole("ADMIN", "SERVICE")
                                                .requestMatchers(HttpMethod.GET, "/api/missions")
                                                .hasAnyRole("ADMIN", "SERVICE")

                                                // Users can track their own orders
                                                .requestMatchers(HttpMethod.GET, "/api/missions/order/*/tracking")
                                                .authenticated()
                                                .anyRequest().authenticated())
                                .addFilterBefore(publicEndpointFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(jwtTokenForwardFilter, UsernamePasswordAuthenticationFilter.class)

                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .bearerTokenResolver(publicEndpointBearerTokenResolver)
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                                                .authenticationEntryPoint(
                                                                new PublicEndpointAwareAuthenticationEntryPoint(
                                                                                jwtAuthenticationEntryPoint)))

                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(
                                                                new PublicEndpointAwareAuthenticationEntryPoint(
                                                                                jwtAuthenticationEntryPoint)));

                return http.build();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000",
                                "http://localhost:5174"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}