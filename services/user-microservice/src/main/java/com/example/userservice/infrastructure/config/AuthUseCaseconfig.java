package com.example.userservice.infrastructure.config;

import com.example.userservice.application.usecases.auth.LoginUseCase;
import com.example.userservice.application.usecases.auth.RegisterUseCase;
import com.example.userservice.domain.port.PasswordEncoderPort;
import com.example.userservice.domain.port.TokenGeneratorPort;
import com.example.userservice.domain.repository.RestaurantRepository;
import com.example.userservice.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthUseCaseconfig {

    @Bean
    RegisterUseCase registerUseCase(UserRepository userRepository, RestaurantRepository restaurantRepository,
            PasswordEncoderPort passwordEncoderPort,
            com.example.userservice.domain.repository.OutboxEventRepository outboxEventRepository,
            com.example.userservice.application.service.EventPayloadSerializer eventPayloadSerializer) {
        return new RegisterUseCase(userRepository, restaurantRepository, passwordEncoderPort, outboxEventRepository,
                eventPayloadSerializer);
    }

    @Bean
    LoginUseCase loginUseCase(UserRepository userRepository, PasswordEncoderPort passwordEncoderPort,
            TokenGeneratorPort tokenGeneratorPort) {
        return new LoginUseCase(userRepository, passwordEncoderPort, tokenGeneratorPort);
    }
}
