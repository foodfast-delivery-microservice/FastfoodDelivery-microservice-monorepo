package com.example.payment.infrastructure.lisener;

import com.example.payment.application.usecase.ProcessPaymentUseCase;
import com.example.payment.infrastructure.config.RabbitMQConfig;
import com.example.payment.application.dto.PaymentRequest;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ObjectMapper objectMapper;
    private final ProcessPaymentUseCase processPaymentUseCase;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handleOrderCreatedEvent(String messageJson) {
        log.info("Received OrderCreated event: {}", messageJson);

        try {
            // Parse JSON string thành PaymentRequest
            // Order service gửi message dưới dạng JSON string (text/plain)
            PaymentRequest request = objectMapper.readValue(messageJson, PaymentRequest.class);
            
            log.info("Parsed PaymentRequest for orderId: {}", request.getOrderId());

            // Create payment with PENDING status (not process payment yet)
            // Payment will be processed later when order is CONFIRMED and user wants to pay
            processPaymentUseCase.createPayment(request);
            log.info("Payment created with PENDING status for orderId: {}", request.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON message. Message: {}", messageJson, e);
            throw new RuntimeException("Failed to parse OrderCreated event", e);
        } catch (Exception e) {
            log.error("Failed to process OrderCreated event. Message: {}", messageJson, e);
            throw new RuntimeException("Failed to process OrderCreated event", e);
        }
    }

}
