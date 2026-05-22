package com.example.notificationservice.interfaces.rest;

import com.example.notificationservice.application.dto.SendGridWebhookEvent;
import com.example.notificationservice.application.usecase.ProcessBounceWebhookUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/sendgrid")
@RequiredArgsConstructor
public class SendGridWebhookController {

    private final ProcessBounceWebhookUseCase processBounceWebhookUseCase;
    private final ObjectMapper objectMapper;

    @Value("${sendgrid.webhook.verification-key:}")
    private String verificationKey;

    @Value("${sendgrid.webhook.signature-validation.enabled:true}")
    private boolean signatureValidationEnabled;

    @PostMapping("/events")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Twilio-Email-Event-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Twilio-Email-Event-Webhook-Timestamp", required = false) String timestamp
    ) {
        log.debug("Received SendGrid webhook, payload length: {}", payload.length());

        // Validate signature if enabled
        if (signatureValidationEnabled) {
            if (signature == null || timestamp == null) {
                log.warn("Missing signature or timestamp in webhook request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing signature headers");
            }

            if (!verifySignature(payload, signature, timestamp)) {
                log.warn("Invalid webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
        }

        try {
            // Parse events array
            List<SendGridWebhookEvent> events = objectMapper.readValue(
                    payload,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SendGridWebhookEvent.class)
            );

            log.info("Processing {} webhook events", events.size());

            // Process each event
            for (SendGridWebhookEvent event : events) {
                try {
                    processBounceWebhookUseCase.execute(event);
                } catch (Exception e) {
                    log.error("Error processing individual webhook event: sgEventId={}", event.getSgEventId(), e);
                    // Continue processing other events
                }
            }

            return ResponseEntity.ok("Events processed");

        } catch (Exception e) {
            log.error("Error parsing webhook payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }
    }

    private boolean verifySignature(String payload, String signature, String timestamp) {
        if (verificationKey == null || verificationKey.isEmpty()) {
            log.warn("Webhook verification key not configured, skipping signature validation");
            return true;
        }

        try {
            String signedPayload = timestamp + payload;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    verificationKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            return expectedSignature.equals(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
}
