package com.example.demo.infracstructor.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class JwtAuthConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter authoritiesConverter;

    public JwtAuthConverter(JwtGrantedAuthoritiesConverter authoritiesConverter) {
        this.authoritiesConverter = authoritiesConverter;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        var authorities = authoritiesConverter.convert(jwt);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}