package com.example.gatewayservice;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtTokenForwardFilter jwtTokenForwardFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        // final PublicEndpointFilter publicEndpointFilter;
        private final PublicEndpointBearerTokenResolver publicEndpointBearerTokenResolver;


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
                                                 // 1. Phải khai báo luật cho ADMIN trước (Cụ thể hơn ưu tiên trước)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/restaurants/admin/all").hasRole("ADMIN")

                                                // 2. Sau đó mới đến luật Public cho các cái còn lại (Tổng quát hơn để sau)
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

                                                // File upload and serving
                                                .requestMatchers(HttpMethod.POST, "/api/v1/upload/**")
                                                .hasAnyRole("ADMIN", "MERCHANT")
                                                .requestMatchers(HttpMethod.GET, "/api/v1/files/**")
                                                .permitAll()

                                                // ORDER & PAYMENT endpoints - specific patterns first
                                                .requestMatchers("/api/v1/orders/merchants/**")
                                                .hasAnyRole("MERCHANT", "ADMIN")
                                                .requestMatchers("/api/v1/orders/**").authenticated()
                                                .requestMatchers("/api/v1/payments/merchants/**")
                                                .hasAnyRole("MERCHANT", "ADMIN")
                                                .requestMatchers("/api/v1/payments/**").authenticated()

                                                .requestMatchers("/actuator/**").permitAll()

                                                // Admin-only endpoints (Drone management)
                                                .requestMatchers(HttpMethod.POST, "/api/v1/drones").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/drones/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/drones/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/v1/drones/assignments/**")
                                                .hasAnyRole("ADMIN", "MERCHANT")

                                                // Drone & mission visibility
                                                .requestMatchers(HttpMethod.GET, "/api/v1/drones/**")
                                                .hasAnyRole("ADMIN", "MERCHANT", "SERVICE")
                                                // Tracking endpoint - cho phép authenticated users (customer có thể xem tracking đơn của mình)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/missions/order/*/tracking")
                                                .authenticated()
                                                // Mission by order ID - cho phép authenticated users (customer có thể xem mission đơn của mình)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/missions/order/**")
                                                .authenticated()
                                                // Other mission endpoints - chỉ ADMIN/SERVICE
                                                .requestMatchers(HttpMethod.GET, "/api/v1/missions/**")
                                                .hasAnyRole("ADMIN", "SERVICE")
                                                .anyRequest().authenticated())
                                //.addFilterBefore(publicEndpointFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(jwtTokenForwardFilter, UsernamePasswordAuthenticationFilter.class)

                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .bearerTokenResolver(publicEndpointBearerTokenResolver)
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(

                                                                                jwtAuthenticationEntryPoint));

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