package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.DeliveryAddressResponse;
import com.example.order_service.application.dto.OrderDetailResponse;
import com.example.order_service.application.dto.OrderItemResponse;
import com.example.order_service.application.dto.UpdateOrderStatusRequest;
import com.example.order_service.domain.exception.MerchantOrderAccessDeniedException;
import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.application.dto.PaymentInfo;
import com.example.order_service.domain.model.EventStatus;
import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.model.OutboxEvent;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.domain.repository.OutboxEventRepository;
import com.example.order_service.domain.repository.PaymentServicePort;
import com.example.order_service.infrastructure.event.OrderRefundRequestEvent;
import com.example.order_service.infrastructure.event.OrderPaidEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentServicePort paymentServicePort;

    @Transactional
    public OrderDetailResponse execute(Long orderId, UpdateOrderStatusRequest request) {
        log.info("Updating order status for orderId: {}, request: {}", orderId, request);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        // Validate status transition
        OrderStatus newStatus = validateAndParseStatus(request.getStatus());
        validateStatusTransition(order.getStatus(), newStatus);

        // Update order status
        updateOrderStatus(order, newStatus);
        order = orderRepository.save(order);

        // Create outbox event
        createStatusChangeEvent(order, newStatus, request.getNote());

        return mapToOrderDetailResponse(order);
    }

    /**
     * Update order status with merchant ownership validation
     * @param orderId Order ID
     * @param merchantId Merchant ID from JWT token
     * @param request Update status request
     * @return Updated order detail
     * @throws OrderValidationException if order does not belong to merchant
     */
    @Transactional
    public OrderDetailResponse executeForMerchant(Long orderId, Long merchantId, UpdateOrderStatusRequest request) {
        log.info("Merchant {} updating order status for orderId: {}, request: {}", merchantId, orderId, request);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        // Validate merchant ownership
        if (!order.getMerchantId().equals(merchantId)) {
            log.warn("Merchant {} attempted to update order {} which belongs to merchant {}", 
                    merchantId, orderId, order.getMerchantId());
            throw new MerchantOrderAccessDeniedException(
                    String.format("Order %d does not belong to merchant %d", orderId, merchantId)
            );
        }

        // Validate status transition
        OrderStatus newStatus = validateAndParseStatus(request.getStatus());
        validateStatusTransition(order.getStatus(), newStatus);

        // Update order status
        updateOrderStatus(order, newStatus);
        order = orderRepository.save(order);

        // Create outbox event
        createStatusChangeEvent(order, newStatus, request.getNote());

        return mapToOrderDetailResponse(order);
    }

    private OrderStatus validateAndParseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new OrderValidationException("Status is required");
        }

        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OrderValidationException("Invalid order status: " + status);
        }
    }

    /**
     * Validate order status transitions theo flow chuẩn nghiệp vụ:
     * 
     * FLOW CHÍNH (Chỉ Drone Delivery):
     * PENDING → CONFIRMED → PAID → PROCESSING → DELIVERING → DELIVERED
     * 
     * RULES:
     * 1. Thanh toán chỉ chấp nhận QR (duy nhất phương thức thanh toán)
     * 2. PROCESSING là bắt buộc sau PAID (chuẩn bị hàng)
     * 3. DELIVERING là duy nhất phương thức giao hàng (drone delivery)
     * 4. SHIPPED không được dùng trong flow chính (chỉ giữ lại để tương thích)
     */
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        Map<OrderStatus, OrderStatus[]> validTransitions = new HashMap<>();
        
        // 1. PENDING: Order mới tạo, chờ merchant xác nhận
        validTransitions.put(OrderStatus.PENDING, new OrderStatus[]{OrderStatus.CONFIRMED, OrderStatus.CANCELLED});
        
        // 2. CONFIRMED: Merchant đã xác nhận, chờ thanh toán
        validTransitions.put(OrderStatus.CONFIRMED, new OrderStatus[]{OrderStatus.PAID, OrderStatus.CANCELLED});
        
        // 3. PAID: Đã thanh toán, BẮT BUỘC phải qua PROCESSING để chuẩn bị hàng
        validTransitions.put(OrderStatus.PAID, new OrderStatus[]{OrderStatus.PROCESSING, OrderStatus.CANCELLED});
        
        // 4. PROCESSING: Đang chuẩn bị hàng, sau đó gán drone để giao hàng
        //    - DELIVERING: Gán drone để giao hàng (duy nhất phương thức giao hàng)
        validTransitions.put(OrderStatus.PROCESSING, new OrderStatus[]{OrderStatus.DELIVERING, OrderStatus.CANCELLED});
        
        // 5. DELIVERING: Drone đang giao hàng (chỉ dùng cho drone delivery)
        validTransitions.put(OrderStatus.DELIVERING, new OrderStatus[]{OrderStatus.DELIVERED, OrderStatus.CANCELLED});
        
        // 6. SHIPPED: Không được dùng trong flow chính (chỉ giữ lại để tương thích)
        //    Hệ thống chỉ dùng drone delivery, không có traditional shipping
        // validTransitions.put(OrderStatus.SHIPPED, new OrderStatus[]{OrderStatus.DELIVERED, OrderStatus.CANCELLED});
        
        // 7. DELIVERED: Đã giao hàng thành công
        validTransitions.put(OrderStatus.DELIVERED, new OrderStatus[]{OrderStatus.REFUNDED});
        
        // 8. CANCELLED: Đã hủy (không thể thay đổi)
        validTransitions.put(OrderStatus.CANCELLED, new OrderStatus[]{});
        
        // 9. REFUNDED: Đã hoàn tiền (không thể thay đổi)
        validTransitions.put(OrderStatus.REFUNDED, new OrderStatus[]{});

        OrderStatus[] allowedTransitions = validTransitions.get(currentStatus);
        if (allowedTransitions == null || !java.util.Arrays.asList(allowedTransitions).contains(newStatus)) {
            throw new OrderValidationException(
                    String.format("Cannot change order status from %s to %s", currentStatus, newStatus)
            );
        }
    }

    private void updateOrderStatus(Order order, OrderStatus newStatus) {
        switch (newStatus) {
            case CONFIRMED:
                order.confirm();
                break;
            case PROCESSING:
                // Order đang được xử lý (chuẩn bị hàng)
                // Ghi nhận thời điểm bắt đầu xử lý để phục vụ báo cáo/thống kê SLA
                order.setStatus(OrderStatus.PROCESSING);
                if (order.getProcessingStartedAt() == null) {
                    order.setProcessingStartedAt(LocalDateTime.now());
                }
                break;
            case DELIVERING:
                // Order đang được giao bởi drone
                order.setStatus(OrderStatus.DELIVERING);
                break;
            case SHIPPED:
                order.markAsShipped();
                break;
            case DELIVERED:
                order.markAsDelivered();
                break;
            case CANCELLED:
                order.cancel();
                break;
            case REFUNDED:
                order.setStatus(OrderStatus.REFUNDED);
                // Publish OrderRefundRequestEvent to payment service
                try {
                    PaymentInfo paymentInfo = paymentServicePort.getPaymentByOrderId(order.getId());
                    createRefundRequestEvent(order, paymentInfo.getPaymentId(), order.getGrandTotal(), null);
                } catch (Exception e) {
                    log.error("Failed to create refund request event for order {}: {}", order.getId(), e.getMessage());
                    // Don't throw exception to avoid rolling back the order status update
                }
                break;
            default:
                throw new OrderValidationException("Unsupported status: " + newStatus);
        }
    }

    private void createStatusChangeEvent(Order order, OrderStatus newStatus, String note) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("orderId", order.getId());
            eventData.put("orderCode", order.getOrderCode());
            eventData.put("userId", order.getUserId());
            eventData.put("oldStatus", order.getStatus().name());
            eventData.put("newStatus", newStatus.name());
            eventData.put("note", note);
            // Format LocalDateTime as String to avoid Jackson JSR310 module requirement
            eventData.put("timestamp", LocalDateTime.now().toString());

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .type("OrderStatusChanged")
                    .payload(new ObjectMapper().writeValueAsString(eventData))
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to create status change event for order {}: {}", order.getId(), e.getMessage());
            // Don't throw exception to avoid rolling back the order status update
        }
    }

    private OrderDetailResponse mapToOrderDetailResponse(Order order) {
        return OrderDetailResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .merchantId(order.getMerchantId())
                .status(order.getStatus().name())
                .currency(order.getCurrency())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getGrandTotal())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .processingStartedAt(order.getProcessingStartedAt())
                .deliveryAddress(mapToDeliveryAddressResponse(order.getDeliveryAddress()))
                .orderItems(order.getOrderItems().stream()
                        .map(this::mapToOrderItemResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private DeliveryAddressResponse mapToDeliveryAddressResponse(
            com.example.order_service.domain.model.DeliveryAddress deliveryAddress) {
        return DeliveryAddressResponse.builder()
                .receiverName(deliveryAddress.getReceiverName())
                .receiverPhone(deliveryAddress.getReceiverPhone())
                .addressLine1(deliveryAddress.getAddressLine1())
                .ward(deliveryAddress.getWard())
                .district(deliveryAddress.getDistrict())
                .city(deliveryAddress.getCity())
                .fullAddress(deliveryAddress.getFullAddress())
                .lat(deliveryAddress.getLat())
                .lng(deliveryAddress.getLng())
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(
            com.example.order_service.domain.model.OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .merchantId(orderItem.getMerchantId())
                .productName(orderItem.getProductName())
                .unitPrice(orderItem.getUnitPrice())
                .quantity(orderItem.getQuantity())
                .lineTotal(orderItem.getLineTotal())
                .build();
    }
    @Transactional
    public void markAsPaid(Long orderId) {
        log.info("Marking order {} as PAID", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        // chỉ cho phép từ PENDING hoặc CONFIRMED sang PAID
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderValidationException(
                    String.format("Cannot mark order %s as PAID from status %s", orderId, order.getStatus())
            );
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        
        // tạo outbox event nếu muốn sync sang service khác (inventory, delivery,…)
        createStatusChangeEvent(order, OrderStatus.PAID, "Payment successful");
        
        // Sau khi thanh toán, tự động chuyển sang PROCESSING để chuẩn bị hàng
        // (Theo flow chuẩn: PAID → PROCESSING)
        // Note: Theo PRD section 10.3, hiện tại dùng Option 2 (Auto)
        // Có thể chuyển sang Option 1 (Manual) nếu merchant cần kiểm soát tốt hơn
        try {
            order.setStatus(OrderStatus.PROCESSING);
            order.setProcessingStartedAt(LocalDateTime.now());
            orderRepository.save(order);
            log.info("✅ Order {} automatically moved to PROCESSING after payment. Processing started at: {}", 
                    orderId, order.getProcessingStartedAt());
            createStatusChangeEvent(order, OrderStatus.PROCESSING, "Tự động chuyển sang chuẩn bị hàng sau khi thanh toán");
        } catch (Exception e) {
            log.warn("⚠️ Could not auto-move order {} to PROCESSING: {}", orderId, e.getMessage());
            // Không throw exception để không rollback việc mark as PAID
        }
        
        // Create OrderPaidEvent for stock deduction
        createOrderPaidEvent(order);
    }

    @Transactional
    public void markAsPaymentFailed(Long orderId, String reason) {
        log.info("Marking order {} as PAYMENT_FAILED. Reason: {}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        // chỉ cho phép từ PENDING sang PAYMENT_FAILED
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderValidationException(
                    String.format("Cannot mark order %s as PAYMENT_FAILED from status %s", orderId, order.getStatus())
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setNote(reason);
        orderRepository.save(order);

        createStatusChangeEvent(order, OrderStatus.CANCELLED, reason);
    }

    private void createRefundRequestEvent(Order order, Long paymentId, java.math.BigDecimal refundAmount, String reason) {
        try {
            OrderRefundRequestEvent eventPayload = new OrderRefundRequestEvent(
                    order.getId(),
                    paymentId,
                    refundAmount,
                    reason
            );

            String payloadJson = new ObjectMapper().writeValueAsString(eventPayload);

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
        }
    }

    private void createOrderPaidEvent(Order order) {
        try {
            // Map orderItems to OrderItemDeduction
            java.util.List<OrderPaidEvent.OrderItemDeduction> orderItemDeductions = order.getOrderItems().stream()
                    .map(item -> OrderPaidEvent.OrderItemDeduction.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .merchantId(item.getMerchantId())
                            .build())
                    .collect(Collectors.toList());

            OrderPaidEvent eventPayload = OrderPaidEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .merchantId(order.getMerchantId())
                    .orderItems(orderItemDeductions)
                    .build();

            String payloadJson = new ObjectMapper().writeValueAsString(eventPayload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .type("OrderPaid")
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(event);
            log.info("[STOCK_DEDUCTION] Created OrderPaid event for orderId: {}, items count: {}", 
                    order.getId(), orderItemDeductions.size());

        } catch (Exception e) {
            log.error("Failed to create OrderPaid event for order {}: {}", order.getId(), e.getMessage());
            // Don't throw exception to avoid rolling back the order status update
            // Event will be retried later via outbox pattern
        }
    }

}
