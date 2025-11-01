package com.example.order_service.infrastructure.security;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    SecurityFilterChain filter(HttpSecurity http, JwtAuthConverter jwtAuthConverter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth


                                // 2. Yêu cầu xác thực cho tất cả các endpoint /orders khác
                                .requestMatchers("/api/v1/orders/**").authenticated()

                                // 3. Các request còn lại (nếu có) cũng cần xác thực
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthConverter) // Bảo Spring dùng converter của bạn
                        )
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
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

