package com.example.gatewayservice;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter để đảm bảo JWT token được forward đến downstream services
 */
@Component
public class JwtTokenForwardFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenForwardFilter.class);

    @Override
    protected void doFilterInternal(@jakarta.annotation.Nonnull HttpServletRequest request, 
                                    @jakarta.annotation.Nonnull HttpServletResponse response, 
                                    @jakarta.annotation.Nonnull FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Log Idempotency-Key header để debug
        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey != null) {
            log.info("=== GATEWAY FILTER: Idempotency-Key header received: {} ===", idempotencyKey);
        } else {
            log.info("=== GATEWAY FILTER: No Idempotency-Key header in request ===");
        }
        
        // Log tất cả headers liên quan đến idempotency
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName.toLowerCase().contains("idempotency") || 
                headerName.toLowerCase().contains("idem")) {
                log.info("Gateway Filter - Header '{}': {}", headerName, request.getHeader(headerName));
            }
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            
            // Log để debug
            log.debug("JWT Token found for request: {} {}", request.getMethod(), request.getRequestURI());
            log.debug("Token subject: {}, role: {}", 
                    jwt.getSubject(), 
                    jwt.getClaimAsString("role"));
            
            // Đảm bảo Authorization header được set (Spring Cloud Gateway WebMVC sẽ forward tự động)
            // Nhưng chúng ta log để debug
            if (request.getHeader("Authorization") == null) {
                log.warn("Authorization header is missing in request, but JWT is present in SecurityContext");
            }
        } else {
            log.debug("No JWT authentication found for request: {} {}", 
                    request.getMethod(), request.getRequestURI());
        }
        
        filterChain.doFilter(request, response);
    }
}

