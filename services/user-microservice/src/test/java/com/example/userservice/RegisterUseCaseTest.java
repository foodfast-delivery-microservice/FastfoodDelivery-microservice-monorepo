package com.example.userservice;

import com.example.userservice.application.DTOs.auth.RegisterRequest;
import com.example.userservice.application.service.DisposableEmailValidator;
import com.example.userservice.application.service.EmailOtpService;
import com.example.userservice.application.service.EventPayloadSerializer;
import com.example.userservice.application.usecases.auth.RegisterUseCase;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.exception.DisposableEmailNotAllowedException;
import com.example.userservice.domain.port.PasswordEncoderPort;
import com.example.userservice.domain.repository.OutboxEventRepository;
import com.example.userservice.domain.repository.RestaurantRepository;
import com.example.userservice.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class RegisterUseCaseTest {

    @Test
    void register_withDisposableEmail_throwsBadRequestException() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RestaurantRepository restaurantRepository = Mockito.mock(RestaurantRepository.class);
        PasswordEncoderPort passwordEncoderPort = Mockito.mock(PasswordEncoderPort.class);
        OutboxEventRepository outboxEventRepository = Mockito.mock(OutboxEventRepository.class);
        EventPayloadSerializer eventPayloadSerializer = Mockito.mock(EventPayloadSerializer.class);
        EmailOtpService emailOtpService = Mockito.mock(EmailOtpService.class);
        DisposableEmailValidator disposableEmailValidator = Mockito.mock(DisposableEmailValidator.class);
        com.example.userservice.application.service.EmailDomainValidator emailDomainValidator = Mockito.mock(com.example.userservice.application.service.EmailDomainValidator.class);

        RegisterUseCase useCase = new RegisterUseCase(
                userRepository,
                restaurantRepository,
                passwordEncoderPort,
                outboxEventRepository,
                eventPayloadSerializer,
                emailOtpService,
                disposableEmailValidator,
                emailDomainValidator
        );

        RegisterRequest request = new RegisterRequest();
        request.setUsername("new-user");
        request.setEmail("temp@10minutemail.com");
        request.setPassword("Password@123");
        request.setRole(User.UserRole.USER.name());

        Mockito.when(userRepository.existsByUsername(eq("new-user"))).thenReturn(false);
        Mockito.when(userRepository.existsByEmail(eq("temp@10minutemail.com"))).thenReturn(false);
        Mockito.when(disposableEmailValidator.isDisposable(eq("temp@10minutemail.com"))).thenReturn(true);

        assertThrows(DisposableEmailNotAllowedException.class, () -> useCase.register(request));

        Mockito.verify(disposableEmailValidator).isDisposable(anyString());
        Mockito.verifyNoInteractions(passwordEncoderPort, outboxEventRepository, eventPayloadSerializer, emailOtpService);
    }
}
