package com.example.notificationservice.infrastructure.messaging.listener;

import com.example.notificationservice.application.dto.InAppNotificationDto;
import com.example.notificationservice.application.usecase.CreateInAppNotificationUseCase;
import com.example.notificationservice.application.dto.OrderConfirmedEventDto;
import com.example.notificationservice.application.usecase.SendOrderConfirmedEmailUseCase;
import com.example.notificationservice.domain.entities.InAppNotification;
import com.example.notificationservice.infrastructure.config.RabbitMQConfig;
import com.example.notificationservice.infrastructure.event.OrderStatusChangedEventPayload;
import com.example.notificationservice.infrastructure.websocket.WebSocketNotificationService;
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
    private final CreateInAppNotificationUseCase createInAppNotificationUseCase;
    private final WebSocketNotificationService webSocketNotificationService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_STATUS_CHANGED_QUEUE)
    public void handleOrderStatusChanged(OrderStatusChangedEventPayload payload) {
        log.info("Received order status changed event: orderId={}, oldStatus={}, newStatus={}",
                payload.getOrderId(), payload.getOldStatus(), payload.getNewStatus());

        try {
            // Chỉ gửi email khi order được CONFIRMED (merchant duyệt)
            if ("CONFIRMED".equalsIgnoreCase(payload.getNewStatus())) {
                log.info("Order {} confirmed by merchant, sending confirmation email", payload.getOrderId());

                // 1. Create in-app notification first
                InAppNotification inAppRecord = null;
                if (payload.getUserId() != null) {
                    try {
                        inAppRecord = createInAppNotificationUseCase.execute(
                                payload.getUserId(),
                                "Đơn hàng đã xác nhận",
                                "Đơn hàng #" + payload.getOrderCode() + " được merchant duyệt",
                                com.example.notificationservice.domain.valueobjects.NotificationType.ORDER_CONFIRMED,
                                String.valueOf(payload.getOrderId())
                        );
                    } catch (Exception e) {
                        log.error("Failed to create in-app notification for order confirmed: orderId={}", payload.getOrderId(), e);
                    }
                }

                // 2. Trigger email sending
                OrderConfirmedEventDto eventDto = OrderConfirmedEventDto.builder()
                        .orderId(payload.getOrderId())
                        .orderCode(payload.getOrderCode())
                        .userId(payload.getUserId())
                        .amount(BigDecimal.ZERO) // TODO: Lấy từ order service nếu cần
                        .timestamp(payload.getTimestamp())
                        .build();

                sendOrderConfirmedEmailUseCase.handle(eventDto);
                log.info("Successfully processed order confirmed event: orderId={}", payload.getOrderId());

                // 3. Push real-time WebSocket notification
                if (inAppRecord != null && payload.getUserId() != null) {
                    try {
                        webSocketNotificationService.pushToUser(payload.getUserId(), toDto(inAppRecord));
                    } catch (Exception e) {
                        log.error("Failed to push real-time notification for order confirmed: orderId={}", payload.getOrderId(), e);
                    }
                }
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

    private InAppNotificationDto toDto(InAppNotification n) {
        if (n == null) return null;
        return InAppNotificationDto.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .referenceId(n.getReferenceId())
                .channel(n.getChannel())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
