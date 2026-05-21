package com.example.notificationservice.infrastructure.messaging.listener;

import com.example.notificationservice.application.dto.InAppNotificationDto;
import com.example.notificationservice.application.dto.PaymentEventDto;
import com.example.notificationservice.application.usecase.CreateInAppNotificationUseCase;
import com.example.notificationservice.application.usecase.SendPaymentFailedEmailUseCase;
import com.example.notificationservice.application.usecase.SendPaymentRefundedEmailUseCase;
import com.example.notificationservice.application.usecase.SendPaymentSuccessEmailUseCase;
import com.example.notificationservice.domain.entities.InAppNotification;
import com.example.notificationservice.domain.port.OrderServicePort;
import com.example.notificationservice.infrastructure.config.RabbitMQConfig;
import com.example.notificationservice.infrastructure.event.PaymentFailedEventPayload;
import com.example.notificationservice.infrastructure.event.PaymentRefundedEventPayload;
import com.example.notificationservice.infrastructure.event.PaymentSuccessEventPayload;
import com.example.notificationservice.infrastructure.websocket.WebSocketNotificationService;
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
    private final CreateInAppNotificationUseCase createInAppNotificationUseCase;
    private final OrderServicePort orderServicePort;
    private final WebSocketNotificationService webSocketNotificationService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(PaymentSuccessEventPayload payload) {
        log.info("Received payment success event: paymentId={}, orderId={}",
                payload.getPaymentId(), payload.getOrderId());

        try {
            // 1. Create in-app notification first
            Long userId = payload.getUserId();
            String orderCode = String.valueOf(payload.getOrderId());
            String amountStr = "";
            try {
                var order = orderServicePort.getOrderById(payload.getOrderId());
                if (order != null) {
                    if (userId == null) userId = order.getUserId();
                    if (order.getOrderCode() != null) orderCode = order.getOrderCode();
                    if (order.getGrandTotal() != null) amountStr = " " + order.getGrandTotal().toString() + "đ";
                }
            } catch (Exception e) {
                log.warn("Failed to fetch order details for in-app notification: orderId={}", payload.getOrderId(), e);
            }

            InAppNotification inAppRecord = null;
            if (userId != null) {
                try {
                    inAppRecord = createInAppNotificationUseCase.execute(
                            userId,
                            "Thanh toán thành công",
                            "Đơn hàng #" + orderCode + " đã thanh toán" + amountStr,
                            com.example.notificationservice.domain.valueobjects.NotificationType.PAYMENT_SUCCESS,
                            String.valueOf(payload.getPaymentId())
                    );
                } catch (Exception e) {
                    log.error("Failed to create in-app notification for payment success: paymentId={}", payload.getPaymentId(), e);
                }
            }

            // 2. Trigger email sending
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

            // 3. Push real-time WebSocket notification
            if (inAppRecord != null && userId != null) {
                try {
                    webSocketNotificationService.pushToUser(userId, toDto(inAppRecord));
                } catch (Exception e) {
                    log.error("Failed to push real-time notification for payment success: paymentId={}", payload.getPaymentId(), e);
                }
            }

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
            // 1. Create in-app notification first
            Long userId = payload.getUserId();
            String orderCode = String.valueOf(payload.getOrderId());
            try {
                var order = orderServicePort.getOrderById(payload.getOrderId());
                if (order != null) {
                    if (userId == null) userId = order.getUserId();
                    if (order.getOrderCode() != null) orderCode = order.getOrderCode();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch order details for in-app notification: orderId={}", payload.getOrderId(), e);
            }

            InAppNotification inAppRecord = null;
            if (userId != null) {
                try {
                    inAppRecord = createInAppNotificationUseCase.execute(
                            userId,
                            "Thanh toán thất bại",
                            "Đơn hàng #" + orderCode + " thanh toán thất bại: " + (payload.getReason() != null ? payload.getReason() : "Lỗi không xác định"),
                            com.example.notificationservice.domain.valueobjects.NotificationType.PAYMENT_FAILED,
                            String.valueOf(payload.getPaymentId())
                    );
                } catch (Exception e) {
                    log.error("Failed to create in-app notification for payment failed: paymentId={}", payload.getPaymentId(), e);
                }
            }

            // 2. Trigger email sending
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

            // 3. Push real-time WebSocket notification
            if (inAppRecord != null && userId != null) {
                try {
                    webSocketNotificationService.pushToUser(userId, toDto(inAppRecord));
                } catch (Exception e) {
                    log.error("Failed to push real-time notification for payment failed: paymentId={}", payload.getPaymentId(), e);
                }
            }

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
            // 1. Create in-app notification first
            Long userId = null;
            String orderCode = String.valueOf(payload.getOrderId());
            String amountStr = "";
            try {
                var order = orderServicePort.getOrderById(payload.getOrderId());
                if (order != null) {
                    userId = order.getUserId();
                    if (order.getOrderCode() != null) orderCode = order.getOrderCode();
                    if (order.getGrandTotal() != null) amountStr = " " + order.getGrandTotal().toString() + "đ";
                }
            } catch (Exception e) {
                log.warn("Failed to fetch order details for in-app notification: orderId={}", payload.getOrderId(), e);
            }

            final Long targetUserId = userId;
            InAppNotification inAppRecord = null;
            if (userId != null) {
                try {
                    inAppRecord = createInAppNotificationUseCase.execute(
                            userId,
                            "Hoàn tiền thành công",
                            "Đơn hàng #" + orderCode + " đã hoàn tiền" + amountStr,
                            com.example.notificationservice.domain.valueobjects.NotificationType.PAYMENT_REFUNDED,
                            String.valueOf(payload.getPaymentId())
                    );
                } catch (Exception e) {
                    log.error("Failed to create in-app notification for payment refunded: paymentId={}", payload.getPaymentId(), e);
                }
            }

            // 2. Trigger email sending
            sendPaymentRefundedEmailUseCase.handle(
                    payload.getPaymentId(),
                    payload.getOrderId(),
                    payload.getReason()
            );
            log.info("Successfully processed payment refunded event: paymentId={}, orderId={}",
                    payload.getPaymentId(), payload.getOrderId());

            // 3. Push real-time WebSocket notification
            if (inAppRecord != null && targetUserId != null) {
                try {
                    webSocketNotificationService.pushToUser(targetUserId, toDto(inAppRecord));
                } catch (Exception e) {
                    log.error("Failed to push real-time notification for payment refunded: paymentId={}", payload.getPaymentId(), e);
                }
            }

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
