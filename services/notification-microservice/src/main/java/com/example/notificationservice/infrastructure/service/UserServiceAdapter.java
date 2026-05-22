package com.example.notificationservice.infrastructure.service;

import com.example.notificationservice.application.dto.UpdateEmailDeliverabilityRequest;
import com.example.notificationservice.application.dto.UserEmailResponse;
import com.example.notificationservice.domain.port.UserServicePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceAdapter implements UserServicePort {

    private final WebClient userWebClient;
    private final WebClient internalUserWebClient;
    private final ObjectMapper objectMapper;

    @Override
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserEmailFallback")
    public UserEmailResponse getUserEmailById(Long userId) {
        try {
            log.info("Fetching user email for userId: {}", userId);

            String responseJson = userWebClient.get()
                    .uri("/{id}/email", userId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(error -> log.error("Error calling User Service: {}", error.getMessage()))
                    .block();

            if (responseJson == null) {
                log.error("User Service returned null response for userId: {}", userId);
                return null;
            }

            log.debug("User Service response: {}", responseJson);

            // Parse ApiResponse wrapper
            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode dataNode = rootNode.get("data");

            if (dataNode == null || dataNode.isNull()) {
                log.warn("User {} not found", userId);
                return null;
            }

            // Extract user data
            Long id = dataNode.has("id") ? dataNode.get("id").asLong() : userId;
            String fullName = dataNode.has("fullName") ? dataNode.get("fullName").asText() : null;
            String email = dataNode.has("email") ? dataNode.get("email").asText() : null;
            Boolean emailUndeliverable = dataNode.has("emailUndeliverable") ? dataNode.get("emailUndeliverable").asBoolean() : false;

            log.info("User email retrieved: userId={}, email={}, undeliverable={}", id, email, emailUndeliverable);

            return UserEmailResponse.builder()
                    .id(id)
                    .fullName(fullName)
                    .email(email)
                    .emailUndeliverable(emailUndeliverable)
                    .build();

        } catch (WebClientResponseException.NotFound ex) {
            log.warn("User {} not found (404)", userId);
            return null;
        } catch (WebClientResponseException ex) {
            log.error("HTTP error when calling User Service: {} - {}", ex.getStatusCode(), ex.getMessage(), ex);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error calling User Service for userId: {}", userId, e);
            return null;
        }
    }

    /**
     * Fallback method when circuit breaker is open or service is unavailable.
     * Returns null to allow graceful degradation.
     */
    @Override
    @CircuitBreaker(name = "userService", fallbackMethod = "updateEmailDeliverabilityFallback")
    public boolean updateEmailDeliverability(Long userId, boolean undeliverable, java.time.LocalDateTime bouncedAt, int bounceIncrement) {
        try {
            UpdateEmailDeliverabilityRequest request = UpdateEmailDeliverabilityRequest.builder()
                    .undeliverable(undeliverable)
                    .bouncedAt(bouncedAt)
                    .bounceIncrement(bounceIncrement)
                    .build();

            String responseJson = internalUserWebClient.patch()
                    .uri("/{id}/deliverability", userId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseJson == null) {
                return false;
            }

            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode statusNode = rootNode.get("status");
            return statusNode != null && "success".equalsIgnoreCase(statusNode.asText());
        } catch (Exception e) {
            log.error("Failed to update email deliverability for userId={}", userId, e);
            return false;
        }
    }

    private UserEmailResponse getUserEmailFallback(Long userId, Exception ex) {
        log.warn("Circuit breaker open or service unavailable for userId: {}. Using fallback.", userId, ex);
        return null;
    }

    private boolean updateEmailDeliverabilityFallback(Long userId, boolean undeliverable,
                                                      java.time.LocalDateTime bouncedAt, int bounceIncrement,
                                                      Exception ex) {
        log.warn("Circuit breaker open when updating deliverability for userId={}", userId, ex);
        return false;
    }
}
