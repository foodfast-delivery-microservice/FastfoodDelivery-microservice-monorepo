package com.example.demo.infracstructor.security;

import org.springframework.http.HttpMethod;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
    SecurityFilterChain filter(HttpSecurity http, JwtAuthConverter jwtAuthConverter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 2. Cho phép XEM (GET) sản phẩm công khai
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                        // 2. Cho Order Service gọi validate (không cần ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/validate").permitAll()
                        // 3. File upload and serving
                        .requestMatchers(HttpMethod.POST, "/api/v1/upload/**").hasAnyRole("ADMIN", "MERCHANT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll()
                        // 4. Yêu cầu ADMIN cho các hành động CUD (Tạo, Sửa, Xóa) sản phẩm
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/merchants/**")
                        .hasAnyRole("ADMIN", "MERCHANT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAnyRole("ADMIN", "MERCHANT")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("ADMIN", "MERCHANT")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasAnyRole("ADMIN", "MERCHANT")

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                    {
                                      "status": 401,
                                      "message": "Unauthorized",
                                      "data": null,
                                      "error": "TOKEN_REQUIRED_OR_INVALID"
                                    }
                                    """);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                    {
                                      "status": 403,
                                      "message": "Forbidden",
                                      "data": null,
                                      "error": "ACCESS_DENIED"
                                    }
                                    """);

                        })
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));
        return http.build();
    }

    @Bean
    JwtAuthConverter jwtAuthConverter(JwtGrantedAuthoritiesConverter grantedConverter) {
        return new JwtAuthConverter(grantedConverter);
    }

    @Bean
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter() {
        return new JwtGrantedAuthoritiesConverter("role"); // hoặc "roles"
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt.base64-secretkey}") String secretKey) {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(secretKey);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.from("HS512"))
                .build();
    }
}
