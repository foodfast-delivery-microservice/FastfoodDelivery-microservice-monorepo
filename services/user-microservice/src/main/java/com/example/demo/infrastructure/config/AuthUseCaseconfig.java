package com.example.demo.infrastructure.config;

import com.example.demo.application.usecase.LoginUseCase;
import com.example.demo.application.usecase.RegisterUseCase;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.infrastructure.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
@RequiredArgsConstructor
public class AuthUseCaseconfig {
    private final UserRepository userRepository;

    @Bean
    RegisterUseCase registerUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder){
        return new RegisterUseCase(userRepository, passwordEncoder);
    }
    @Bean
    LoginUseCase loginUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder, SecurityUtil securityUtil){
        return new LoginUseCase(userRepository,passwordEncoder,securityUtil);
    }
}
