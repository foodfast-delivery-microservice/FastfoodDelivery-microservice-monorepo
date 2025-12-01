package com.example.payment.infrastructure.lisener;

import com.example.payment.application.usecase.ProcessPaymentUseCase;
import com.example.payment.infrastructure.config.RabbitMQConfig;
import com.example.payment.application.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ProcessPaymentUseCase processPaymentUseCase;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handleOrderCreatedEvent(PaymentRequest request) {
        log.info("Received OrderCreated event for orderId: {}", request.getOrderId());

        // Create payment with PENDING status (not process payment yet)
        // Payment will be processed later when order is CONFIRMED and user wants to pay
        processPaymentUseCase.createPayment(request);
        log.info("Payment created with PENDING status for orderId: {}", request.getOrderId());
    }

}
