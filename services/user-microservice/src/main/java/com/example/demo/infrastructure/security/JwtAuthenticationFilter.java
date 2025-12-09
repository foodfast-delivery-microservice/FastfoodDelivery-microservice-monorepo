package com.example.demo.infrastructure.security;

import com.nimbusds.jose.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;

    @Value("${app.jwt.base64-secretkey}")
    private String jwtKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String requestPath = request.getRequestURI();
        
        System.out.println("🌐 [JWT Filter] Request path: " + requestPath);
        System.out.println("🔑 [JWT Filter] Authorization header present: " + (authHeader != null));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("⚠️ [JWT Filter] No valid Authorization header, skipping authentication");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            byte[] keyBytes = Base64.from(jwtKey).decode();
            SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, MacAlgorithm.HS512.getName());

            NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();

            Jwt jwt = decoder.decode(token);

            String username = jwt.getSubject();
            String roleFromToken = jwt.getClaimAsString("role");
            System.out.println("🔑 [JWT Filter] Decoded token - username: " + username + ", role from token: " + roleFromToken);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                System.out.println("👤 [JWT Filter] Loaded user - username: " + userDetails.getUsername());
                System.out.println("🔐 [JWT Filter] User authorities: " + userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("✅ [JWT Filter] Authentication set successfully");
            } else {
                System.out.println("⚠️ [JWT Filter] Username is null or authentication already exists");
            }

        } catch (JwtException e) {
            // Nếu token sai hoặc hết hạn thì bỏ qua
            System.out.println("❌ [JWT Filter] Invalid JWT: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}

