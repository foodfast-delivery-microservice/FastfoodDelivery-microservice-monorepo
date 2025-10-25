package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.DeliveryAddressResponse;
import com.example.order_service.application.dto.OrderDetailResponse;
import com.example.order_service.application.dto.OrderItemResponse;
import com.example.order_service.application.dto.UpdateOrderStatusRequest;
import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.model.EventStatus;
import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.model.OutboxEvent;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.domain.repository.OutboxEventRepository;
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

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        Map<OrderStatus, OrderStatus[]> validTransitions = new HashMap<>();
        validTransitions.put(OrderStatus.PENDING, new OrderStatus[]{OrderStatus.CONFIRMED, OrderStatus.CANCELLED});
        validTransitions.put(OrderStatus.CONFIRMED, new OrderStatus[]{OrderStatus.SHIPPED, OrderStatus.CANCELLED});
        validTransitions.put(OrderStatus.SHIPPED, new OrderStatus[]{OrderStatus.DELIVERED});
        validTransitions.put(OrderStatus.DELIVERED, new OrderStatus[]{OrderStatus.REFUNDED});
        validTransitions.put(OrderStatus.CANCELLED, new OrderStatus[]{});
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
            eventData.put("timestamp", LocalDateTime.now());

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
                .status(order.getStatus().name())
                .currency(order.getCurrency())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getGrandTotal())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
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
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(
            com.example.order_service.domain.model.OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .productName(orderItem.getProductName())
                .unitPrice(orderItem.getUnitPrice())
                .quantity(orderItem.getQuantity())
                .lineTotal(orderItem.getLineTotal())
                .build();
    }
}
