package com.example.gatewayservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // <-- đúng gói Spring
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenForwardFilter jwtTokenForwardFilter;

    public SecurityConfig(JwtTokenForwardFilter jwtTokenForwardFilter) {
        this.jwtTokenForwardFilter = jwtTokenForwardFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthConverter jwtAuthConverter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
//                        -- USER --
                                .requestMatchers("/api/v1/auth/**").permitAll()

                                .requestMatchers(HttpMethod.POST,"/api/v1/users/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET,"/api/v1/users/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH,"/api/v1/users/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE,"/api/v1/users/**").hasRole("ADMIN")

//                                  -- PRODUCT --
                                // Merchant endpoints require MERCHANT or ADMIN role
                                .requestMatchers("/api/v1/products/merchants/**").hasAnyRole("MERCHANT", "ADMIN")
                                // 2. Cho phép XEM (GET) sản phẩm công khai
                                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                                // 3. Yêu cầu ADMIN hoặc MERCHANT cho các hành động CUD (Tạo, Sửa, Xóa) sản phẩm
                                .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAnyRole("ADMIN", "MERCHANT")
                                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("ADMIN", "MERCHANT")
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasAnyRole("ADMIN", "MERCHANT")

                                // 5. Yêu cầu đã đăng nhập (authenticated) để ĐẶT HÀNG
                                // Merchant endpoints require MERCHANT or ADMIN role
                                .requestMatchers("/api/v1/orders/merchants/**").hasAnyRole("MERCHANT", "ADMIN")
                                // Other order endpoints require authentication (for USER to create orders)
                                .requestMatchers("/api/v1/orders/**").authenticated()
                                // Merchant endpoints require MERCHANT or ADMIN role
                                .requestMatchers("/api/v1/payments/merchants/**").hasAnyRole("MERCHANT", "ADMIN")
                                // Other payment endpoints require authentication
                                .requestMatchers("/api/v1/payments/**").authenticated()


                        .anyRequest().authenticated()
                )
                .addFilterAfter(jwtTokenForwardFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));
        return http.build();
    }
}
