package com.example.demo.application.usecase;

import com.example.demo.domain.exception.InvalidCredentialException;
import com.example.demo.domain.exception.UserNotFoundException;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.infrastructure.security.SecurityUtil;
import com.example.demo.interfaces.rest.dto.auth.LoginRequest;
import com.example.demo.interfaces.rest.dto.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
public class LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;

    public LoginResponse login (LoginRequest loginRequest) {
        // 1. Tìm user theo username
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(()-> new UserNotFoundException(loginRequest.getUsername()));

        // 2. Kiểm tra password
        if  (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialException();
        }

        // 3. Sinh token
        String accessToken = securityUtil.createAccessToken(user);
        String refreshToken = securityUtil.createRefreshToken(user.getUsername());

        return new LoginResponse(user.getId().toString(),user.getUsername(),accessToken,refreshToken);
    }
}
