package com.example.notificationservice.infrastructure.messaging.listener;

import com.example.notificationservice.application.dto.OrderConfirmedEventDto;
import com.example.notificationservice.application.usecase.SendOrderConfirmedEmailUseCase;
import com.example.notificationservice.infrastructure.config.RabbitMQConfig;
import com.example.notificationservice.infrastructure.event.OrderStatusChangedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Listener for order status changed events from RabbitMQ.
 * Only sends email when order status changes to CONFIRMED.
 * <p>
 * Error Handling Strategy:
 * - Throws exception to trigger RabbitMQ retry mechanism and DLQ
 * - This ensures failed notifications are not lost and can be retried
 * - Invalid events are logged and exception is thrown to send to DLQ
 * - Note: Non-CONFIRMED status changes are logged but don't throw exceptions (no email needed)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusChangedListener {

    private final SendOrderConfirmedEmailUseCase sendOrderConfirmedEmailUseCase;

    @RabbitListener(queues = RabbitMQConfig.ORDER_STATUS_CHANGED_QUEUE)
    public void handleOrderStatusChanged(OrderStatusChangedEventPayload payload) {
        log.info("Received order status changed event: orderId={}, oldStatus={}, newStatus={}",
                payload.getOrderId(), payload.getOldStatus(), payload.getNewStatus());

        try {
            // Chỉ gửi email khi order được CONFIRMED (merchant duyệt)
            if ("CONFIRMED".equalsIgnoreCase(payload.getNewStatus())) {
                log.info("Order {} confirmed by merchant, sending confirmation email", payload.getOrderId());

                OrderConfirmedEventDto eventDto = OrderConfirmedEventDto.builder()
                        .orderId(payload.getOrderId())
                        .orderCode(payload.getOrderCode())
                        .userId(payload.getUserId())
                        .amount(BigDecimal.ZERO) // TODO: Lấy từ order service nếu cần
                        .timestamp(payload.getTimestamp())
                        .build();

                sendOrderConfirmedEmailUseCase.handle(eventDto);
                log.info("Successfully processed order confirmed event: orderId={}", payload.getOrderId());
            } else {
                log.debug("Order status changed to {}, no email action required", payload.getNewStatus());
                // No exception thrown for non-CONFIRMED status - this is expected behavior
            }

        } catch (IllegalArgumentException e) {
            // Validation errors - log and throw to send to DLQ for manual review
            log.error("Invalid order status changed event (sending to DLQ): orderId={}, newStatus={}",
                    payload.getOrderId(), payload.getNewStatus(), e);
            throw new RuntimeException("Invalid order status changed event", e);
        } catch (Exception e) {
            // Other errors - log and throw to trigger retry/DLQ
            log.error("Error processing order status changed event (sending to DLQ): orderId={}, newStatus={}",
                    payload.getOrderId(), payload.getNewStatus(), e);
            throw e;
        }
    }
}
