package com.example.userservice.infrastructure.config;

import com.example.userservice.application.usecases.auth.LoginUseCase;
import com.example.userservice.application.usecases.auth.RegisterUseCase;
import com.example.userservice.domain.repository.RestaurantRepository;
import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.infrastructure.security.SecurityUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class AuthUseCaseconfig {

    @Bean
    RegisterUseCase registerUseCase(UserRepository userRepository, RestaurantRepository restaurantRepository, PasswordEncoder passwordEncoder){
        return new RegisterUseCase(userRepository, restaurantRepository, passwordEncoder);
    }
    @Bean
    LoginUseCase loginUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder, SecurityUtil securityUtil){
        return new LoginUseCase(userRepository,passwordEncoder,securityUtil);
    }
}
