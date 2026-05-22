package com.example.userservice.infrastructure.config;

import com.example.userservice.application.usecases.user.*;
import com.example.userservice.application.usecases.restaurant.*;
import com.example.userservice.domain.port.PasswordEncoderPort;
import com.example.userservice.domain.repository.RestaurantRepository;
import com.example.userservice.domain.repository.UserRepository;
import com.example.userservice.domain.repository.OutboxEventRepository;
import com.example.userservice.application.service.EventPayloadSerializer;
import com.example.userservice.infrastructure.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class UserUseCaseConfig {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final EventPayloadSerializer eventPayloadSerializer;
    private final PasswordEncoderPort passwordEncoderPort;


    @Bean
    public GetUserByIdUseCase getUserByIdUseCase (){

        return new GetUserByIdUseCase(userRepository);
    }

    @Bean
    public GetAllUsersUseCase getAllUserUseCase() {
        return new GetAllUsersUseCase(userRepository);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(UserRepository userRepository, RestaurantRepository restaurantRepository, PasswordEncoderPort passwordEncoderPort, GeocodingService geocodingService) {
        return new CreateUserUseCase(userRepository, restaurantRepository, passwordEncoderPort, geocodingService);
    }

    @Bean
    public ValidateUserAccessUseCase validateUserAccessUseCase() {
        return new ValidateUserAccessUseCase();
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase() {
        return new UpdateUserUseCase(userRepository, outboxEventRepository, eventPayloadSerializer, validateUserAccessUseCase());
    }

    @Bean
    public DeleteUserByIdUseCase deleteUserByIdUseCase() {
        return new DeleteUserByIdUseCase(userRepository);
    }

    @Bean
    public ChangePasswordUseCase changePasswordUseCase() {
        return new ChangePasswordUseCase(userRepository, passwordEncoderPort, validateUserAccessUseCase());
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
    public UpdateEmailDeliverabilityUseCase updateEmailDeliverabilityUseCase() {
        return new UpdateEmailDeliverabilityUseCase(userRepository);
    }

    @Bean
    public UpdateRestaurantUseCase updateRestaurantUseCaseBean() {
        return new UpdateRestaurantUseCase(restaurantRepository);
    }
}
