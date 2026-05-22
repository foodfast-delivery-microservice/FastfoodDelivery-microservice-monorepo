package com.example.userservice;

import com.example.userservice.application.DTOs.auth.RegisterRequest;
import com.example.userservice.application.service.DisposableEmailValidator;
import com.example.userservice.application.service.EmailDomainValidator;
import com.example.userservice.application.service.EmailOtpService;
import com.example.userservice.application.service.EventPayloadSerializer;
import com.example.userservice.application.usecases.auth.RegisterUseCase;
import com.example.userservice.domain.entities.User;
import com.example.userservice.domain.exception.DisposableEmailNotAllowedException;
import com.example.userservice.domain.port.PasswordEncoderPort;
import com.example.userservice.domain.repository.OutboxEventRepository;
import com.example.userservice.domain.repository.RestaurantRepository;
import com.example.userservice.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUseCase Tests")
class RegisterUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private EventPayloadSerializer eventPayloadSerializer;

    @Mock
    private EmailOtpService emailOtpService;

    @Mock
    private DisposableEmailValidator disposableEmailValidator;

    @Mock
    private EmailDomainValidator emailDomainValidator;

    @InjectMocks
    private RegisterUseCase registerUseCase;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("new-user");
        validRequest.setEmail("user@example.com");
        validRequest.setPassword("Password@123");
        validRequest.setRole(User.UserRole.USER.name());
    }

    @Test
    @DisplayName("Should throw DisposableEmailNotAllowedException when email is disposable")
    void register_withDisposableEmail_throwsDisposableEmailNotAllowedException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new-user");
        request.setEmail("temp@10minutemail.com");
        request.setPassword("Password@123");
        request.setRole(User.UserRole.USER.name());

        when(userRepository.existsByUsername(eq("new-user"))).thenReturn(false);
        when(userRepository.existsByEmail(eq("temp@10minutemail.com"))).thenReturn(false);
        when(disposableEmailValidator.isDisposable(eq("temp@10minutemail.com"))).thenReturn(true);

        assertThrows(DisposableEmailNotAllowedException.class, () -> registerUseCase.register(request));

        verify(disposableEmailValidator).isDisposable(anyString());
        verifyNoInteractions(passwordEncoderPort, outboxEventRepository, eventPayloadSerializer, emailOtpService);
    }
}
