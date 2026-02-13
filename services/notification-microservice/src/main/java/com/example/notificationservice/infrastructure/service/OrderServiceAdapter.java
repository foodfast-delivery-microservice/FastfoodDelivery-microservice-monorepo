package com.example.notificationservice.infrastructure.service;

import com.example.notificationservice.application.dto.OrderDetailResponse;
import com.example.notificationservice.domain.port.OrderServicePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceAdapter implements OrderServicePort {

    private final WebClient internalOrderWebClient;
    private final ObjectMapper objectMapper;

    @Override
    public OrderDetailResponse getOrderById(Long orderId) {
        try {
            log.info("Fetching order details for orderId: {}", orderId);

            // Use internal API endpoint
            String responseJson = internalOrderWebClient.get()
                    .uri("/api/internal/orders/{orderId}", orderId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(error -> log.error("Error calling Order Service: {}", error.getMessage()))
                    .block();

            if (responseJson == null) {
                log.error("Order Service returned null response for orderId: {}", orderId);
                return null;
            }

            log.debug("Order Service response: {}", responseJson);

            // Parse response - could be direct object or wrapped in ApiResponse
            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode dataNode = rootNode.has("data") ? rootNode.get("data") : rootNode;

            if (dataNode == null || dataNode.isNull()) {
                log.warn("Order {} not found", orderId);
                return null;
            }

            // Extract order data
            Long id = dataNode.has("id") ? dataNode.get("id").asLong() : orderId;
            String orderCode = dataNode.has("orderCode") ? dataNode.get("orderCode").asText() : null;
            Long userId = dataNode.has("userId") ? dataNode.get("userId").asLong() : null;
            BigDecimal grandTotal = dataNode.has("grandTotal") && !dataNode.get("grandTotal").isNull()
                    ? new BigDecimal(dataNode.get("grandTotal").asText())
                    : null;

            log.info("Order details retrieved: orderId={}, userId={}, grandTotal={}", id, userId, grandTotal);

            return OrderDetailResponse.builder()
                    .id(id)
                    .orderCode(orderCode)
                    .userId(userId)
                    .grandTotal(grandTotal)
                    .build();

        } catch (WebClientResponseException.NotFound ex) {
            log.warn("Order {} not found (404)", orderId);
            return null;
        } catch (WebClientResponseException ex) {
            log.error("HTTP error when calling Order Service: {} - {}", ex.getStatusCode(), ex.getMessage(), ex);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error calling Order Service for orderId: {}", orderId, e);
            return null;
        }
    }
}
