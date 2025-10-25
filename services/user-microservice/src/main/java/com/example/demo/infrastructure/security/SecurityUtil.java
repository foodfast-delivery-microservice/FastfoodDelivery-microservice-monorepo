package com.example.demo.infrastructure.security;

import com.example.demo.domain.model.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jwt.*;


@Service
@RequiredArgsConstructor
public class SecurityUtil {
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.access.expiration-in-seconds}")
    private long jwtAccessExpiration;

    @Value("${app.jwt.refresh.expiration-in-seconds}")
    private long jwtRefreshExpiration;

    public static final String ROLE_KEY = "role";
    public static final String REFRESH_TOKEN = "refresh_token";

    // Hash Algorithm
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    public String createAccessToken (User user){
        Instant now = Instant.now();
        Instant validity = now.plus(jwtAccessExpiration, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(user.getUsername())
                .claim(ROLE_KEY, user.getRole().name())
                .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String createRefreshToken(String subject) {
        Instant now = Instant.now();
        Instant validity = now.plus(jwtRefreshExpiration, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(subject)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }


}
