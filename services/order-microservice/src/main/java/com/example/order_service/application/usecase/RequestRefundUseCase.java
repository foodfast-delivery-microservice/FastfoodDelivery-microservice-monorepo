package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.PaymentInfo;
import com.example.order_service.application.dto.RefundRequest;
import com.example.order_service.application.dto.RefundResponse;
import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.model.EventStatus;
import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.model.OutboxEvent;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.domain.repository.OutboxEventRepository;
import com.example.order_service.domain.repository.PaymentServicePort;
import com.example.order_service.infrastructure.event.OrderRefundRequestEvent;
import com.example.order_service.infrastructure.event.OrderRefundedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestRefundUseCase {

    private final OrderRepository orderRepository;
    private final PaymentServicePort paymentServicePort;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public RefundResponse execute(Long orderId, RefundRequest request) {
        // 1. Validate order exists
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        // Tự động lấy refundAmount từ order.grandTotal nếu không được truyền
        BigDecimal refundAmount = request.getRefundAmount() != null 
                ? request.getRefundAmount() 
                : order.getGrandTotal();

        if (request.getRefundAmount() == null) {
            log.info("[REFUND_REQUEST] Refund amount not provided, using order grand total: {}", refundAmount);
        }

        log.info("[REFUND_REQUEST] Processing refund request - orderId: {}, refundAmount: {}, reason: {}", 
                orderId, refundAmount, request.getReason());

        // Audit log: Log order and user info
        log.info("[REFUND_AUDIT] Refund request initiated - orderId: {}, userId: {}, merchantId: {}, orderStatus: {}, refundAmount: {}, reason: {}", 
                orderId, order.getUserId(), order.getMerchantId(), order.getStatus(), refundAmount, request.getReason());

        // 2. Validate order status must be DELIVERED
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new OrderValidationException(
                    String.format("Cannot refund order with status: %s. Order must be DELIVERED", order.getStatus())
            );
        }

        // 3. Validate order already has payment
        PaymentInfo paymentInfo;
        try {
            paymentInfo = paymentServicePort.getPaymentByOrderId(orderId);
        } catch (RuntimeException e) {
            log.error("Failed to get payment for orderId: {}", orderId, e);
            throw new OrderValidationException("Order does not have a payment. Cannot process refund.");
        }

        // 4. Validate payment status must be SUCCESS
        if (!"SUCCESS".equalsIgnoreCase(paymentInfo.getStatus())) {
            throw new OrderValidationException(
                    String.format("Cannot refund payment with status: %s. Payment must be SUCCESS", paymentInfo.getStatus())
            );
        }

        // 5. Validate refundAmount = order.grandTotal (full refund only)
        if (refundAmount.compareTo(order.getGrandTotal()) != 0) {
            throw new OrderValidationException(
                    String.format("Refund amount (%s) must equal order grand total (%s) for full refund", 
                            refundAmount, order.getGrandTotal())
            );
        }

        // 6. Update order status to REFUNDED
        order.setStatus(OrderStatus.REFUNDED);
        order = orderRepository.save(order);

        // 7. Create and publish OrderRefundRequestEvent to outbox
        createRefundRequestEvent(order, paymentInfo.getPaymentId(), refundAmount, request.getReason());

        // 8. Create and publish OrderRefundedEvent to inventory service (restore stock)
        createOrderRefundedEvent(order, request.getReason());

        // Audit log: Log successful refund request
        log.info("[REFUND_AUDIT] Refund request processed successfully - orderId: {}, userId: {}, merchantId: {}, paymentId: {}, refundAmount: {}, reason: {}", 
                orderId, order.getUserId(), order.getMerchantId(), paymentInfo.getPaymentId(), refundAmount, request.getReason());
        log.info("[REFUND_REQUEST] Refund request processed successfully for orderId: {}", orderId);

        return RefundResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .message("Refund request processed successfully")
                .refundAmount(refundAmount)
                .refundedAt(LocalDateTime.now())
                .build();
    }

    private void createRefundRequestEvent(Order order, Long paymentId, BigDecimal refundAmount, String reason) {
        try {
            OrderRefundRequestEvent eventPayload = new OrderRefundRequestEvent(
                    order.getId(),
                    paymentId,
                    refundAmount,
                    reason
            );

            String payloadJson = objectMapper.writeValueAsString(eventPayload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .type("OrderRefundRequest")
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(event);
            log.info("Created OrderRefundRequest event for orderId: {}, paymentId: {}", order.getId(), paymentId);

        } catch (Exception e) {
            log.error("Failed to create refund request event for order {}: {}", order.getId(), e.getMessage());
            // Don't throw exception to avoid rolling back the order status update
            // Event will be retried later via outbox pattern
        }
    }

    private void createOrderRefundedEvent(Order order, String reason) {
        try {
            // Map orderItems to OrderItemRefund
            java.util.List<OrderRefundedEvent.OrderItemRefund> orderItemRefunds = order.getOrderItems().stream()
                    .map(item -> OrderRefundedEvent.OrderItemRefund.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .merchantId(item.getMerchantId())
                            .build())
                    .collect(Collectors.toList());

            OrderRefundedEvent eventPayload = OrderRefundedEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .merchantId(order.getMerchantId())
                    .reason(reason)
                    .orderItems(orderItemRefunds)
                    .build();

            String payloadJson = objectMapper.writeValueAsString(eventPayload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .type("OrderRefunded")
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(event);
            log.info("[STOCK_RESTORE] Created OrderRefunded event for orderId: {}, items count: {}", 
                    order.getId(), orderItemRefunds.size());

        } catch (Exception e) {
            log.error("Failed to create OrderRefunded event for order {}: {}", order.getId(), e.getMessage());
            // Don't throw exception to avoid rolling back the order status update
            // Event will be retried later via outbox pattern
        }
    }
}

