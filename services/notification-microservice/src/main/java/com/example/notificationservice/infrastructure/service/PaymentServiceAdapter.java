package com.example.notificationservice.infrastructure.service;

import com.example.notificationservice.application.dto.PaymentDetailResponse;
import com.example.notificationservice.domain.port.PaymentServicePort;
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
public class PaymentServiceAdapter implements PaymentServicePort {

    private final WebClient paymentWebClient;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentDetailResponse getPaymentByOrderId(Long orderId) {
        try {
            log.info("Fetching payment details for orderId: {}", orderId);

            String responseJson = paymentWebClient.get()
                    .uri("/order/{orderId}", orderId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(error -> log.error("Error calling Payment Service: {}", error.getMessage()))
                    .block();

            if (responseJson == null) {
                log.error("Payment Service returned null response for orderId: {}", orderId);
                return null;
            }

            log.debug("Payment Service response: {}", responseJson);

            // Parse response - could be direct object or wrapped in ApiResponse
            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode dataNode = rootNode.has("data") ? rootNode.get("data") : rootNode;

            if (dataNode == null || dataNode.isNull()) {
                log.warn("Payment for order {} not found", orderId);
                return null;
            }

            // Extract payment data
            Long id = dataNode.has("id") ? dataNode.get("id").asLong() : null;
            Long orderIdFromResponse = dataNode.has("orderId") ? dataNode.get("orderId").asLong() : orderId;
            BigDecimal amount = dataNode.has("amount") && !dataNode.get("amount").isNull()
                    ? new BigDecimal(dataNode.get("amount").asText())
                    : null;
            String currency = dataNode.has("currency") ? dataNode.get("currency").asText() : null;

            log.info("Payment details retrieved: paymentId={}, orderId={}, amount={}", id, orderIdFromResponse, amount);

            return PaymentDetailResponse.builder()
                    .id(id)
                    .orderId(orderIdFromResponse)
                    .amount(amount)
                    .currency(currency)
                    .build();

        } catch (WebClientResponseException.NotFound ex) {
            log.warn("Payment for order {} not found (404)", orderId);
            return null;
        } catch (WebClientResponseException ex) {
            log.error("HTTP error when calling Payment Service: {} - {}", ex.getStatusCode(), ex.getMessage(), ex);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error calling Payment Service for orderId: {}", orderId, e);
            return null;
        }
    }
}
