package com.example.gatewayservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // <-- đúng gói Spring
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                                // 2. Cho phép XEM (GET) sản phẩm công khai
                                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                                // 3. Yêu cầu ADMIN cho các hành động CUD (Tạo, Sửa, Xóa) sản phẩm
                                .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")

                                // 5. Yêu cầu đã đăng nhập (authenticated) để ĐẶT HÀNG
                                .requestMatchers("/api/v1/orders/**").authenticated()
                                .requestMatchers("/api/v1/payments/**").authenticated()


                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));
        return http.build();
    }
}
