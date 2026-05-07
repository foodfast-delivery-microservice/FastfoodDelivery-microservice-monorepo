package com.example.notificationservice.application.usecase;

import com.example.notificationservice.application.dto.PaymentEventDto;
import com.example.notificationservice.application.dto.UserEmailResponse;
import com.example.notificationservice.domain.port.EmailSenderPort;
import com.example.notificationservice.domain.port.UserServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendPaymentFailedEmailUseCaseTest {

    @Mock
    private UserServicePort userServicePort;

    @Mock
    private EmailSenderPort emailSenderPort;

    @InjectMocks
    private SendPaymentFailedEmailUseCase useCase;

    private PaymentEventDto eventDto;
    private UserEmailResponse userResponse;

    @BeforeEach
    void setUp() {
        eventDto = PaymentEventDto.builder()
                .paymentId(1L)
                .orderId(100L)
                .userId(10L)
                .amount(new BigDecimal("50000"))
                .paymentTime(Instant.now())
                .status("FAILED")
                .failureReason("Insufficient funds")
                .build();

        userResponse = UserEmailResponse.builder()
                .id(10L)
                .fullName("Test User")
                .email("test@example.com")
                .build();
    }

    @Test
    void shouldSendEmailWhenUserExists() {
        when(userServicePort.getUserEmailById(10L)).thenReturn(userResponse);

        useCase.handle(eventDto);

        verify(userServicePort).getUserEmailById(10L);
        verify(emailSenderPort).sendPaymentFailedEmail(eq(eventDto), eq("test@example.com"), eq(10L));
    }

    @Test
    void shouldThrowExceptionWhenEventIsNull() {
        assertThrows(IllegalArgumentException.class, () -> useCase.handle(null));
        verify(userServicePort, never()).getUserEmailById(any());
        verify(emailSenderPort, never()).sendPaymentFailedEmail(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {
        eventDto.setUserId(null);

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(eventDto));

        verify(userServicePort, never()).getUserEmailById(any());
        verify(emailSenderPort, never()).sendPaymentFailedEmail(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        when(userServicePort.getUserEmailById(10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(eventDto));

        verify(userServicePort).getUserEmailById(10L);
        verify(emailSenderPort, never()).sendPaymentFailedEmail(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsBlank() {
        userResponse.setEmail("");
        when(userServicePort.getUserEmailById(10L)).thenReturn(userResponse);

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(eventDto));

        verify(userServicePort).getUserEmailById(10L);
        verify(emailSenderPort, never()).sendPaymentFailedEmail(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsNull() {
        userResponse.setEmail(null);
        when(userServicePort.getUserEmailById(10L)).thenReturn(userResponse);

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(eventDto));

        verify(userServicePort).getUserEmailById(10L);
        verify(emailSenderPort, never()).sendPaymentFailedEmail(any(), any(), any());
    }
}
