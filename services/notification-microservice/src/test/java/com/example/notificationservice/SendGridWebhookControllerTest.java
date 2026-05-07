package com.example.notificationservice;

import com.example.notificationservice.application.dto.SendGridWebhookEvent;
import com.example.notificationservice.application.usecase.ProcessBounceWebhookUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "sendgrid.webhook.verification-key=test-key-123",
    "sendgrid.webhook.signature-validation.enabled=true",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
class SendGridWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessBounceWebhookUseCase processBounceWebhookUseCase;

    private String verificationKey = "test-key-123";

    @BeforeEach
    void setUp() {
        reset(processBounceWebhookUseCase);
    }

    @Test
    @DisplayName("Should accept webhook with valid signature")
    void handleWebhook_validSignature_acceptsEvent() throws Exception {
        SendGridWebhookEvent event = new SendGridWebhookEvent();
        event.setSgEventId("evt_123");
        event.setEvent("bounce");
        event.setType("permanent");
        event.setEmail("test@example.com");
        event.setUserId("123");

        String payload = objectMapper.writeValueAsString(List.of(event));
        String timestamp = "1620000000";
        String signature = generateSignature(payload, timestamp);

        mockMvc.perform(post("/api/webhooks/sendgrid/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("X-Twilio-Email-Event-Webhook-Signature", signature)
                .header("X-Twilio-Email-Event-Webhook-Timestamp", timestamp))
                .andExpect(status().isOk());

        verify(processBounceWebhookUseCase).execute(any(SendGridWebhookEvent.class));
    }

    @Test
    @DisplayName("Should reject webhook with invalid signature")
    void handleWebhook_invalidSignature_rejectsEvent() throws Exception {
        SendGridWebhookEvent event = new SendGridWebhookEvent();
        event.setSgEventId("evt_456");
        event.setEvent("bounce");
        event.setEmail("test@example.com");

        String payload = objectMapper.writeValueAsString(List.of(event));
        String timestamp = "1620000000";
        String invalidSignature = "invalid-signature";

        mockMvc.perform(post("/api/webhooks/sendgrid/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("X-Twilio-Email-Event-Webhook-Signature", invalidSignature)
                .header("X-Twilio-Email-Event-Webhook-Timestamp", timestamp))
                .andExpect(status().isUnauthorized());

        verify(processBounceWebhookUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Should reject webhook with missing signature header")
    void handleWebhook_missingSignature_rejectsEvent() throws Exception {
        SendGridWebhookEvent event = new SendGridWebhookEvent();
        event.setSgEventId("evt_789");
        event.setEvent("bounce");
        event.setEmail("test@example.com");

        String payload = objectMapper.writeValueAsString(List.of(event));

        mockMvc.perform(post("/api/webhooks/sendgrid/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized());

        verify(processBounceWebhookUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Should process multiple events in single webhook")
    void handleWebhook_multipleEvents_processesAll() throws Exception {
        SendGridWebhookEvent event1 = new SendGridWebhookEvent();
        event1.setSgEventId("evt_1");
        event1.setEvent("bounce");
        event1.setEmail("user1@example.com");

        SendGridWebhookEvent event2 = new SendGridWebhookEvent();
        event2.setSgEventId("evt_2");
        event2.setEvent("bounce");
        event2.setEmail("user2@example.com");

        String payload = objectMapper.writeValueAsString(List.of(event1, event2));
        String timestamp = "1620000000";
        String signature = generateSignature(payload, timestamp);

        mockMvc.perform(post("/api/webhooks/sendgrid/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("X-Twilio-Email-Event-Webhook-Signature", signature)
                .header("X-Twilio-Email-Event-Webhook-Timestamp", timestamp))
                .andExpect(status().isOk());

        verify(processBounceWebhookUseCase, times(2)).execute(any(SendGridWebhookEvent.class));
    }

    @Test
    @DisplayName("Should handle invalid JSON payload")
    void handleWebhook_invalidJson_returnsBadRequest() throws Exception {
        String invalidPayload = "{ invalid json }";
        String timestamp = "1620000000";
        String signature = generateSignature(invalidPayload, timestamp);

        mockMvc.perform(post("/api/webhooks/sendgrid/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload)
                .header("X-Twilio-Email-Event-Webhook-Signature", signature)
                .header("X-Twilio-Email-Event-Webhook-Timestamp", timestamp))
                .andExpect(status().isBadRequest());

        verify(processBounceWebhookUseCase, never()).execute(any());
    }

    private String generateSignature(String payload, String timestamp) throws Exception {
        String signedPayload = timestamp + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                verificationKey.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
