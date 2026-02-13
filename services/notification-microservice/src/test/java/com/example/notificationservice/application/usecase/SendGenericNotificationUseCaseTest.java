package com.example.notificationservice.application.usecase;

import com.example.notificationservice.application.dto.NotificationEvent;
import com.example.notificationservice.domain.entities.Notification;
import com.example.notificationservice.domain.port.EmailSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendGenericNotificationUseCaseTest {

    @Mock
    private EmailSenderPort emailSenderPort;

    @InjectMocks
    private SendGenericNotificationUseCase useCase;

    private NotificationEvent event;

    @BeforeEach
    void setUp() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        event = NotificationEvent.builder()
                .eventType("PAYMENT_SUCCESS")
                .recipient("test@example.com")
                .template("payment-success")
                .data(data)
                .build();
    }

    @Test
    void shouldSendEmailWhenEventIsValid() {
        useCase.handle(event);

        verify(emailSenderPort).sendGenericNotification(any(Notification.class));
    }

    @Test
    void shouldThrowExceptionWhenEventIsNull() {
        assertThrows(IllegalArgumentException.class, () -> useCase.handle(null));
        verify(emailSenderPort, never()).sendGenericNotification(any());
    }

    @Test
    void shouldThrowExceptionWhenRecipientIsInvalid() {
        event.setRecipient("invalid-email");

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(event));
        verify(emailSenderPort, never()).sendGenericNotification(any());
    }

    @Test
    void shouldThrowExceptionWhenTemplateIsBlank() {
        event.setTemplate("");

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(event));
        verify(emailSenderPort, never()).sendGenericNotification(any());
    }

    @Test
    void shouldThrowExceptionWhenEventTypeIsBlank() {
        event.setEventType("");

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(event));
        verify(emailSenderPort, never()).sendGenericNotification(any());
    }

    @Test
    void shouldPropagateExceptionWhenEmailSendingFails() {
        doThrow(new RuntimeException("Email sending failed"))
                .when(emailSenderPort).sendGenericNotification(any(Notification.class));

        assertThrows(RuntimeException.class, () -> useCase.handle(event));
    }

    @Test
    void shouldHandleEventWithoutData() {
        event.setData(null);

        assertDoesNotThrow(() -> useCase.handle(event));
        verify(emailSenderPort).sendGenericNotification(any(Notification.class));
    }
}
