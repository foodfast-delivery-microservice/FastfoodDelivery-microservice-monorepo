package com.example.notificationservice.application.usecase;

import com.example.notificationservice.application.dto.OrderConfirmedEventDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendOrderConfirmedEmailUseCaseTest {

    @Mock
    private UserServicePort userServicePort;

    @Mock
    private EmailSenderPort emailSenderPort;

    @InjectMocks
    private SendOrderConfirmedEmailUseCase useCase;

    private OrderConfirmedEventDto eventDto;
    private UserEmailResponse userResponse;

    @BeforeEach
    void setUp() {
        eventDto = OrderConfirmedEventDto.builder()
                .orderId(100L)
                .orderCode("ORD123")
                .userId(10L)
                .amount(new BigDecimal("50000"))
                .timestamp("2026-02-13T10:00:00")
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
        verify(emailSenderPort).sendOrderConfirmedEmail(eq(eventDto), eq("test@example.com"), eq(10L));
    }

    @Test
    void shouldThrowExceptionWhenEventIsNull() {
        assertThrows(IllegalArgumentException.class, () -> useCase.handle(null));
        verify(userServicePort, never()).getUserEmailById(any());
        verify(emailSenderPort, never()).sendOrderConfirmedEmail(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {
        eventDto.setUserId(null);

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(eventDto));

        verify(userServicePort, never()).getUserEmailById(any());
        verify(emailSenderPort, never()).sendOrderConfirmedEmail(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        when(userServicePort.getUserEmailById(10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(eventDto));

        verify(userServicePort).getUserEmailById(10L);
        verify(emailSenderPort, never()).sendOrderConfirmedEmail(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsBlank() {
        userResponse.setEmail("");
        when(userServicePort.getUserEmailById(10L)).thenReturn(userResponse);

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(eventDto));

        verify(userServicePort).getUserEmailById(10L);
        verify(emailSenderPort, never()).sendOrderConfirmedEmail(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsNull() {
        userResponse.setEmail(null);
        when(userServicePort.getUserEmailById(10L)).thenReturn(userResponse);

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(eventDto));

        verify(userServicePort).getUserEmailById(10L);
        verify(emailSenderPort, never()).sendOrderConfirmedEmail(any(), any(), any());
    }
}
