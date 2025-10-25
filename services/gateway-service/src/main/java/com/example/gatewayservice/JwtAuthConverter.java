package com.example.gatewayservice;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;

public class JwtAuthConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private final String roleClaim;

    public JwtAuthConverter() { this("role"); }
    public JwtAuthConverter(String roleClaim) { this.roleClaim = roleClaim; }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        String role = jwt.getClaimAsString(roleClaim);
        if (role != null && !role.isBlank()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        }
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
