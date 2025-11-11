package com.example.payment.infrastructure.lisener;

import com.example.payment.application.usecase.ProcessPaymentUseCase;

import com.example.payment.domain.repository.OutboxEventRepository;
import com.example.payment.infrastructure.config.RabbitMQConfig;
import com.example.payment.application.dto.PaymentRequest;


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
    public void handleOrderCreatedEvent(PaymentRequest request) {
        log.info("Received new message for orderId: {}", request.getOrderId()); // Log request trực tiếp

        try {

            boolean success = processPaymentUseCase.execute(request);

            if (success) {
                log.info(" Payment successfully processed for orderId: {}", request.getOrderId());
            } else {
                log.warn(" Payment failed or already processed for orderId: {}", request.getOrderId());
            }

        } catch (Exception e) {
            log.error("Failed to parse OrderCreatedEvent: {}", request.getOrderId(), e);
        }
    }

}
