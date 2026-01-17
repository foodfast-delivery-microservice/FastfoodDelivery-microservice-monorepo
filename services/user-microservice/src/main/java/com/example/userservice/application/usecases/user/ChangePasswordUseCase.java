package com.example.userservice.application.usecases.user;

import com.example.userservice.domain.exception.InvalidCredentialException;
import com.example.userservice.domain.exception.InvalidId;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.application.DTOs.user.ChangePasswordRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ValidateUserAccessUseCase validateUserAccessUseCase;

    @Transactional
    public User execute(Long userId, ChangePasswordRequest changePasswordRequest, Authentication authentication) {
        // Validate: User can only change their own password (unless ADMIN)
        validateUserAccessUseCase.execute(userId, authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(()->new InvalidId(userId));

        // xác thực mật khẩu cũ
        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new InvalidCredentialException();
        }

        // đổi mật khẩu
        user.changePassword(changePasswordRequest.getNewPassword(), passwordEncoder);

        // lưu lại
        return userRepository.save(user);
    }
}
