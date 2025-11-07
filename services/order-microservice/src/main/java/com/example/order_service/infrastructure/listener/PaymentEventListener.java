package com.example.order_service.infrastructure.listener;

import com.example.order_service.application.usecase.UpdateOrderStatusUseCase;
import com.example.order_service.infrastructure.config.RabbitMQConfig;
import com.example.order_service.infrastructure.event.PaymentFailedEvent;
import com.example.order_service.infrastructure.event.PaymentSuccessEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final ObjectMapper objectMapper;
    private final UpdateOrderStatusUseCase update; // bạn đã có sẵn service này để update order

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(String message) {
        try {
            PaymentSuccessEvent event = objectMapper.readValue(message, PaymentSuccessEvent.class);
            log.info("Received PAYMENT_SUCCESS for orderId: {}", event.getOrderId());
            update.markAsPaid(event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process PAYMENT_SUCCESS event: {}", message, e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(String message) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
            log.info("Received PAYMENT_FAILED for orderId: {}", event.getOrderId());
            update.markAsPaymentFailed(event.getOrderId(), event.getReason());
        } catch (Exception e) {
            log.error("Failed to process PAYMENT_FAILED event: {}", message, e);
        }
    }


}
