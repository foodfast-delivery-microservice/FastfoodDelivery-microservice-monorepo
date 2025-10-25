package com.example.demo.infrastructure.config;

import com.example.demo.application.usecase.*;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.infrastructure.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class UserUseCaseConfig {

    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;


    @Bean
    public GetUserByIdUseCase getUserByIdUseCase (){

        return new GetUserByIdUseCase(userRepository);
    }

    @Bean
    public GetAllUsersUseCase getAllUserUseCase ()
    {
        return new GetAllUsersUseCase(userRepository);
    }

    @Bean
    public CreateUserUseCase createUserUseCase (UserRepository userRepository, PasswordEncoder passwordEncoder){
        return new CreateUserUseCase(userRepository, passwordEncoder);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase (){

        return new UpdateUserUseCase(userRepository,eventPublisher);
    }

    @Bean
    public DeleteUserByIdUseCase deleteUserByIdUseCase (){
        return new DeleteUserByIdUseCase(userRepository);
    }

    @Bean
    public ChangePasswordUseCase changePasswordUseCase (){
        return new ChangePasswordUseCase(userRepository,passwordEncoder);
    }
}
