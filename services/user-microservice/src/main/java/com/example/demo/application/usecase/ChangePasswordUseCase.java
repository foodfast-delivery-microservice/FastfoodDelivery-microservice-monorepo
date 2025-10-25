package com.example.demo.application.usecase;

import com.example.demo.domain.exception.InvalidCredentialException;
import com.example.demo.domain.exception.InvalidId;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.interfaces.rest.dto.user.ChangePasswordRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User execute(Long userId, ChangePasswordRequest changePasswordRequest){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new InvalidId(userId));

        // xác thực mật khẩu cũ
        if (passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())){
            throw new InvalidCredentialException();
        }

        // đổi mật khẩu
        user.changePassword(changePasswordRequest.getNewPassword(), passwordEncoder);

        // lưu lại
        return userRepository.save(user);
    }
}
