package com.example.demo.infracstructor.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String roleClaim; // ví dụ: "role" hoặc "roles"

    public JwtGrantedAuthoritiesConverter() {
        this("role"); // mặc định dùng claim "role"
    }

    public JwtGrantedAuthoritiesConverter(String roleClaim) {
        this.roleClaim = roleClaim;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Trường hợp claim là chuỗi "ADMIN"/"USER"
        String role = jwt.getClaimAsString(roleClaim);
        if (role != null && !role.isBlank()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        }

        // (tuỳ chọn) Nếu bạn đổi sang mảng roles: ["ADMIN","USER"], xử lý thêm:
        // List<String> roles = jwt.getClaimAsStringList("roles");
        // if (roles != null) for (String r : roles) authorities.add(new SimpleGrantedAuthority("ROLE_"+r.toUpperCase()));

        return authorities;
    }
}