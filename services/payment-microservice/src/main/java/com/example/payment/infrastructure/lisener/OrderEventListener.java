package com.example.payment.infrastructure.lisener;

import com.example.payment.application.usecase.ProcessPaymentUseCase;

import com.example.payment.domain.repository.OutboxEventRepository;
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
    private final OutboxEventRepository outboxEventRepository;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handleOrderCreatedEvent(String messagePayload) {
        log.info("Received new message: {}", messagePayload);

        try {
            PaymentRequest request = objectMapper.readValue(messagePayload, PaymentRequest.class);
            boolean success = processPaymentUseCase.execute(request);

            if (success) {
                log.info(" Payment successfully processed for orderId: {}", request.getOrderId());
            } else {
                log.warn(" Payment failed or already processed for orderId: {}", request.getOrderId());
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse OrderCreatedEvent: {}", messagePayload, e);
        } catch (Exception e) {
            log.error("Unexpected error during payment processing: {}", messagePayload, e);
        }
    }

}
