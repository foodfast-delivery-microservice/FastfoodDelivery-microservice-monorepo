package com.example.demo.infrastructure.config;

import com.example.demo.application.usecase.*;
import com.example.demo.domain.repository.RestaurantRepository;
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
    private final RestaurantRepository restaurantRepository;
    private final EventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;


    @Bean
    public GetUserByIdUseCase getUserByIdUseCase (){

        return new GetUserByIdUseCase(userRepository);
    }

    @Bean
    public GetAllUsersUseCase getAllUserUseCase() {
        return new GetAllUsersUseCase(userRepository);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(UserRepository userRepository, RestaurantRepository restaurantRepository, PasswordEncoder passwordEncoder) {
        return new CreateUserUseCase(userRepository, restaurantRepository, passwordEncoder);
    }

    @Bean
    public ValidateUserAccessUseCase validateUserAccessUseCase() {
        return new ValidateUserAccessUseCase();
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase() {
        return new UpdateUserUseCase(userRepository, eventPublisher, validateUserAccessUseCase());
    }

    @Bean
    public DeleteUserByIdUseCase deleteUserByIdUseCase() {
        return new DeleteUserByIdUseCase(userRepository);
    }

    @Bean
    public ChangePasswordUseCase changePasswordUseCase() {
        return new ChangePasswordUseCase(userRepository, passwordEncoder, validateUserAccessUseCase());
    }

    @Bean
    public GetRestaurantsUseCase getRestaurantsUseCase() {
        return new GetRestaurantsUseCase(restaurantRepository);
    }

    @Bean
    public GetRestaurantByIdUseCase getRestaurantByIdUseCase() {
        return new GetRestaurantByIdUseCase(restaurantRepository);
    }

    @Bean
    public GetRestaurantByMerchantIdUseCase getRestaurantByMerchantIdUseCase() {
        return new GetRestaurantByMerchantIdUseCase(restaurantRepository);
    }

    @Bean
    public UpdateRestaurantUseCase updateRestaurantUseCaseBean() {
        return new UpdateRestaurantUseCase(restaurantRepository);
    }
}
