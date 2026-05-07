package com.example.notificationservice;

import com.example.notificationservice.application.dto.SendGridWebhookEvent;
import com.example.notificationservice.application.usecase.ProcessBounceWebhookUseCase;
import com.example.notificationservice.domain.port.UserServicePort;
import com.example.notificationservice.domain.port.WebhookEventPort;
import com.example.notificationservice.domain.repository.EmailNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessBounceWebhookUseCaseTest {

    @Mock
    private WebhookEventPort webhookEventPort;

    @Mock
    private UserServicePort userServicePort;

    @Mock
    private EmailNotificationRepository emailNotificationRepository;

    private ProcessBounceWebhookUseCase processBounceWebhookUseCase;

    @BeforeEach
    void setUp() {
        processBounceWebhookUseCase = new ProcessBounceWebhookUseCase(
                webhookEventPort,
                userServicePort,
                emailNotificationRepository
        );
    }

    @Test
    @DisplayName("Should skip duplicate webhook event")
    void execute_duplicateEvent_skipsProcessing() {
        SendGridWebhookEvent event = new SendGridWebhookEvent();
        event.setSgEventId("evt_123");
        event.setEvent("bounce");
        event.setEmail("test@example.com");

        when(webhookEventPort.isEventProcessed("evt_123")).thenReturn(true);

        processBounceWebhookUseCase.execute(event);

        verify(webhookEventPort).isEventProcessed("evt_123");
        verify(webhookEventPort, never()).recordEventProcessed(anyString(), anyString(), anyString(), any(), any(), anyString());
        verify(userServicePort, never()).updateEmailDeliverability(anyLong(), anyBoolean(), any(), anyInt());
    }

    @Test
    @DisplayName("Should process permanent bounce and mark user as undeliverable")
    void execute_permanentBounce_marksUserUndeliverable() {
        SendGridWebhookEvent event = new SendGridWebhookEvent();
        event.setSgEventId("evt_456");
        event.setEvent("bounce");
        event.setType("permanent");
        event.setEmail("user@example.com");
        event.setUserId("123");
        event.setTimestamp(1620000000L);

        when(webhookEventPort.isEventProcessed("evt_456")).thenReturn(false);
        when(userServicePort.updateEmailDeliverability(eq(123L), eq(true), any(), eq(1))).thenReturn(true);

        processBounceWebhookUseCase.execute(event);

        verify(webhookEventPort).isEventProcessed("evt_456");
        verify(userServicePort).updateEmailDeliverability(eq(123L), eq(true), any(), eq(1));
        verify(webhookEventPort).recordEventProcessed(
                eq("evt_456"),
                eq("bounce"),
                eq("user@example.com"),
                eq(123L),
                isNull(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should process temporary bounce without marking user as undeliverable")
    void execute_temporaryBounce_doesNotMarkUserUndeliverable() {
        SendGridWebhookEvent event = new SendGridWebhookEvent();
        event.setSgEventId("evt_789");
        event.setEvent("bounce");
        event.setType("temporary");
        event.setEmail("user@example.com");
        event.setUserId("456");

        when(webhookEventPort.isEventProcessed("evt_789")).thenReturn(false);

        processBounceWebhookUseCase.execute(event);

        verify(webhookEventPort).isEventProcessed("evt_789");
        verify(userServicePort, never()).updateEmailDeliverability(anyLong(), anyBoolean(), any(), anyInt());
        verify(webhookEventPort).recordEventProcessed(
                eq("evt_789"),
                eq("bounce"),
                eq("user@example.com"),
                eq(456L),
                isNull(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should handle missing sgEventId gracefully")
    void execute_missingSgEventId_skipsProcessing() {
        SendGridWebhookEvent event = new SendGridWebhookEvent();
        event.setSgEventId(null);
        event.setEvent("bounce");
        event.setEmail("test@example.com");

        processBounceWebhookUseCase.execute(event);

        verify(webhookEventPort, never()).isEventProcessed(anyString());
        verify(webhookEventPort, never()).recordEventProcessed(anyString(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("Should handle invalid userId format gracefully")
    void execute_invalidUserId_processesWithoutUserUpdate() {
        SendGridWebhookEvent event = new SendGridWebhookEvent();
        event.setSgEventId("evt_999");
        event.setEvent("bounce");
        event.setType("permanent");
        event.setEmail("user@example.com");
        event.setUserId("invalid");

        when(webhookEventPort.isEventProcessed("evt_999")).thenReturn(false);

        processBounceWebhookUseCase.execute(event);

        verify(webhookEventPort).isEventProcessed("evt_999");
        verify(userServicePort, never()).updateEmailDeliverability(anyLong(), anyBoolean(), any(), anyInt());
        verify(webhookEventPort).recordEventProcessed(
                eq("evt_999"),
                eq("bounce"),
                eq("user@example.com"),
                isNull(),
                isNull(),
                anyString()
        );
    }
}
