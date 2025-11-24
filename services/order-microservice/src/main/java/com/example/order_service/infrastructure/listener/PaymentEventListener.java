package com.example.order_service.infrastructure.listener;

import com.example.order_service.application.usecase.UpdateOrderStatusUseCase;
import com.example.order_service.application.dto.UpdateOrderStatusRequest;
import com.example.order_service.infrastructure.config.RabbitMQConfig;
import com.example.order_service.infrastructure.event.PaymentFailedEvent;
import com.example.order_service.infrastructure.event.PaymentRefundedEvent;
import com.example.order_service.infrastructure.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final UpdateOrderStatusUseCase update; // bạn đã có sẵn service này để update order

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        try {
            log.info("Received PAYMENT_SUCCESS for orderId: {}", event.getOrderId());
            update.markAsPaid(event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process PAYMENT_SUCCESS event: {}", event, e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        try {
            log.info("Received PAYMENT_FAILED for orderId: {}", event.getOrderId());
            update.markAsPaymentFailed(event.getOrderId(), event.getReason());
        } catch (Exception e) {
            log.error("Failed to process PAYMENT_FAILED event: {}", event, e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REFUNDED_QUEUE)
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        try {
            log.info("Received PAYMENT_REFUNDED for orderId: {}, paymentId: {}", event.getOrderId(), event.getPaymentId());
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status("REFUNDED")
                    .note(event.getReason())
                    .build();
            update.execute(event.getOrderId(), request);
        } catch (Exception e) {
            log.error("Failed to process PAYMENT_REFUNDED event: {}", event, e);
        }
    }

}
