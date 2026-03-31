package com.example.notificationservice.infrastructure.messaging.listener;

import com.example.notificationservice.application.dto.PaymentEventDto;
import com.example.notificationservice.application.usecase.SendPaymentFailedEmailUseCase;
import com.example.notificationservice.application.usecase.SendPaymentRefundedEmailUseCase;
import com.example.notificationservice.application.usecase.SendPaymentSuccessEmailUseCase;
import com.example.notificationservice.infrastructure.config.RabbitMQConfig;
import com.example.notificationservice.infrastructure.event.PaymentFailedEventPayload;
import com.example.notificationservice.infrastructure.event.PaymentRefundedEventPayload;
import com.example.notificationservice.infrastructure.event.PaymentSuccessEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Listener for payment-related events from RabbitMQ.
 * Delegates to use cases for business logic.
 * <p>
 * Error Handling Strategy:
 * - Throws exception to trigger RabbitMQ retry mechanism and DLQ
 * - This ensures failed notifications are not lost and can be retried
 * - Invalid events are logged and exception is thrown to send to DLQ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final SendPaymentSuccessEmailUseCase sendPaymentSuccessEmailUseCase;
    private final SendPaymentFailedEmailUseCase sendPaymentFailedEmailUseCase;
    private final SendPaymentRefundedEmailUseCase sendPaymentRefundedEmailUseCase;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(PaymentSuccessEventPayload payload) {
        log.info("Received payment success event: paymentId={}, orderId={}",
                payload.getPaymentId(), payload.getOrderId());

        try {
            PaymentEventDto eventDto = PaymentEventDto.builder()
                    .paymentId(payload.getPaymentId())
                    .orderId(payload.getOrderId())
                    .userId(payload.getUserId())
                    .paymentTime(Instant.now())
                    .status("SUCCESS")
                    .build();

            sendPaymentSuccessEmailUseCase.handle(eventDto);
            log.info("Successfully processed payment success event: paymentId={}, orderId={}",
                    payload.getPaymentId(), payload.getOrderId());

        } catch (IllegalArgumentException e) {
            // Validation errors - log and throw to send to DLQ for manual review
            log.error("Invalid payment success event (sending to DLQ): paymentId={}, orderId={}",
                    payload.getPaymentId(), payload.getOrderId(), e);
            throw new RuntimeException("Invalid payment success event", e);
        } catch (Exception e) {
            // Other errors - log and throw to trigger retry/DLQ
            log.error("Error processing payment success event (sending to DLQ): paymentId={}, orderId={}",
                    payload.getPaymentId(), payload.getOrderId(), e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(PaymentFailedEventPayload payload) {
        log.info("Received payment failed event: paymentId={}, orderId={}, reason={}",
                payload.getPaymentId(), payload.getOrderId(), payload.getReason());

        try {
            PaymentEventDto eventDto = PaymentEventDto.builder()
                    .paymentId(payload.getPaymentId())
                    .orderId(payload.getOrderId())
                    .userId(payload.getUserId())
                    .paymentTime(Instant.now())
                    .status("FAILED")
                    .failureReason(payload.getReason())
                    .build();

            sendPaymentFailedEmailUseCase.handle(eventDto);
            log.info("Successfully processed payment failed event: paymentId={}, orderId={}",
                    payload.getPaymentId(), payload.getOrderId());

        } catch (IllegalArgumentException e) {
            // Validation errors - log and throw to send to DLQ for manual review
            log.error("Invalid payment failed event (sending to DLQ): paymentId={}, orderId={}",
                    payload.getPaymentId(), payload.getOrderId(), e);
            throw new RuntimeException("Invalid payment failed event", e);
        } catch (Exception e) {
            // Other errors - log and throw to trigger retry/DLQ
            log.error("Error processing payment failed event (sending to DLQ): paymentId={}, orderId={}",
                    payload.getPaymentId(), payload.getOrderId(), e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REFUNDED_QUEUE)
    public void handlePaymentRefunded(PaymentRefundedEventPayload payload) {
        log.info("Received payment refunded event: paymentId={}, orderId={}, reason={}",
                payload.getPaymentId(), payload.getOrderId(), payload.getReason());

        try {
            sendPaymentRefundedEmailUseCase.handle(
                    payload.getPaymentId(),
                    payload.getOrderId(),
                    payload.getReason()
            );
            log.info("Successfully processed payment refunded event: paymentId={}, orderId={}",
                    payload.getPaymentId(), payload.getOrderId());

        } catch (IllegalArgumentException e) {
            // Validation errors - log and throw to send to DLQ for manual review
            log.error("Invalid payment refunded event (sending to DLQ): paymentId={}, orderId={}",
                    payload.getPaymentId(), payload.getOrderId(), e);
            throw new RuntimeException("Invalid payment refunded event", e);
        } catch (Exception e) {
            // Other errors - log and throw to trigger retry/DLQ
            log.error("Error processing payment refunded event (sending to DLQ): paymentId={}, orderId={}",
                    payload.getPaymentId(), payload.getOrderId(), e);
            throw e;
        }
    }
}
