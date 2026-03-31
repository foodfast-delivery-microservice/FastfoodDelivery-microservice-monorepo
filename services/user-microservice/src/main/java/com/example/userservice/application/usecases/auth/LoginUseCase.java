package com.example.userservice.application.usecases.auth;

import com.example.userservice.domain.exception.AccountNotApprovedException;
import com.example.userservice.domain.exception.EmailNotVerifiedException;
import com.example.userservice.domain.exception.InvalidCredentialException;
import com.example.userservice.domain.exception.UserNotFoundException;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.port.PasswordEncoderPort;
import com.example.userservice.domain.port.TokenGeneratorPort;
import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.application.DTOs.auth.LoginRequest;
import com.example.userservice.application.DTOs.auth.LoginResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenGeneratorPort tokenGeneratorPort;

    public LoginResponse login (LoginRequest loginRequest) {
        // 1. Tìm user theo username
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(()-> new UserNotFoundException(loginRequest.getUsername()));

        // 2. Kiểm tra password
        if  (!passwordEncoderPort.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialException();
        }

        if (user.getRole() == User.UserRole.MERCHANT && !user.isApproved()) {
            throw new AccountNotApprovedException(user.getId());
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(user.getEmail());
        }

        // 3. Sinh token
        String accessToken = tokenGeneratorPort.createAccessToken(user);
        String refreshToken = tokenGeneratorPort.createRefreshToken(user.getUsername());

        return new LoginResponse(user.getId().toString(),user.getUsername(),accessToken,refreshToken);
    }
}
