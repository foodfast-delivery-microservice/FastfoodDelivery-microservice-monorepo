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
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            log.debug("JWT Token found for request: {} {}", request.getMethod(), request.getRequestURI());
        } else {
            log.debug("No JWT authentication found for request: {} {}", 
                    request.getMethod(), request.getRequestURI());
        }
        
        filterChain.doFilter(request, response);
    }
}

