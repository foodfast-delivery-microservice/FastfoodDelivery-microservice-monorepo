package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.CreateOrderRequest;
import com.example.order_service.application.dto.OrderResponse;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.model.*;
import com.example.order_service.domain.repository.IdempotencyKeyRepository;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public OrderResponse execute(CreateOrderRequest request, String idempotencyKey) {
        log.info("Creating order for user: {}", request.getUserId());

        // Validate request
        validateRequest(request);

        // Handle idempotency if key is provided
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            // Check if request already processed
            if (idempotencyKeyRepository.existsByUserIdAndIdemKey(request.getUserId(), idempotencyKey)) {
                IdempotencyKey existingKey = idempotencyKeyRepository
                        .findByUserIdAndIdemKey(request.getUserId(), idempotencyKey)
                        .orElseThrow(() -> new OrderValidationException("Idempotency key not found"));

                Order existingOrder = orderRepository.findById(existingKey.getOrderId())
                        .orElseThrow(() -> new OrderValidationException("Order not found"));

                return mapToResponse(existingOrder);
            }
        }

        // Validate external services
        validateUserExists(request.getUserId());
        validateProducts(request.getOrderItems());

        // Create order
        Order order = createOrder(request);
        order = orderRepository.save(order);

        // Save idempotency key if provided
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            String requestHash = calculateRequestHash(request);
            IdempotencyKey idemKey = IdempotencyKey.builder()
                    .userId(request.getUserId())
                    .idemKey(idempotencyKey)
                    .requestHash(requestHash)
                    .orderId(order.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            idempotencyKeyRepository.save(idemKey);
        }

        // Create outbox event
        createOutboxEvent(order, "OrderCreated");

        log.info("Order created successfully: {}", order.getOrderCode());
        return mapToResponse(order);
    }

    private void validateUserExists(Long userId) {
        // TODO: Implement user validation when user service is available
        // For now, just validate that userId is positive
        if (userId == null || userId <= 0) {
            throw new OrderValidationException("Invalid user ID: " + userId);
        }
        log.debug("User validation passed for user: {}", userId);
    }

    private void validateProducts(java.util.List<CreateOrderRequest.OrderItemRequest> orderItems) {
        // TODO: Implement product validation when product service is available
        // For now, just validate basic product data
        for (CreateOrderRequest.OrderItemRequest item : orderItems) {
            if (item.getProductId() == null || item.getProductId().trim().isEmpty()) {
                throw new OrderValidationException("Product ID cannot be empty");
            }
            if (item.getProductName() == null || item.getProductName().trim().isEmpty()) {
                throw new OrderValidationException("Product name cannot be empty");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new OrderValidationException("Unit price must be positive");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderValidationException("Quantity must be positive");
            }
        }
        log.debug("Product validation passed for {} items", orderItems.size());
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new OrderValidationException("Invalid user ID: " + request.getUserId());
        }

        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new OrderValidationException("Order items cannot be empty");
        }

        if (request.getDeliveryAddress() == null) {
            throw new OrderValidationException("Delivery address is required");
        }

        log.debug("Request validation passed for user: {}", request.getUserId());
    }

    private Order createOrder(CreateOrderRequest request) {
        String orderCode = generateOrderCode();

        Order order = Order.builder()
                .orderCode(orderCode)
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .currency("VND")
                .discount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO)
                .shippingFee(request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO)
                .note(request.getNote())
                .deliveryAddress(mapToDeliveryAddress(request.getDeliveryAddress()))
                .createdAt(LocalDateTime.now())
                .build();

        // Add order items
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getOrderItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName(itemRequest.getProductName())
                    .unitPrice(itemRequest.getUnitPrice())
                    .quantity(itemRequest.getQuantity())
                    .build();
            order.addOrderItem(orderItem);
        }

        // Calculate totals
        order.calculateTotals();

        return order;
    }

    private DeliveryAddress mapToDeliveryAddress(CreateOrderRequest.DeliveryAddressRequest request) {
        return DeliveryAddress.builder()
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .addressLine1(request.getAddressLine1())
                .ward(request.getWard())
                .district(request.getDistrict())
                .city(request.getCity())
                .build();
    }

    private String generateOrderCode() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String calculateRequestHash(CreateOrderRequest request) {
        try {
            String data = request.getUserId() + request.getOrderItems().toString() +
                    request.getDeliveryAddress().toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculating request hash", e);
        }
    }

    private void createOutboxEvent(Order order, String eventType) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("Order")
                .aggregateId(order.getId().toString())
                .type(eventType)
                .payload(createEventPayload(order))
                .status(EventStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();
        outboxEventRepository.save(event);
    }

    private String createEventPayload(Order order) {
        // Simple JSON payload - in real implementation, use proper JSON library
        return String.format("{\"orderId\":%d,\"orderCode\":\"%s\",\"userId\":%d,\"status\":\"%s\"}",
                order.getId(), order.getOrderCode(), order.getUserId(), order.getStatus());
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
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
                .deliveryAddress(mapToDeliveryAddressResponse(order.getDeliveryAddress()))
                .orderItems(order.getOrderItems().stream()
                        .map(this::mapToOrderItemResponse)
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderResponse.DeliveryAddressResponse mapToDeliveryAddressResponse(DeliveryAddress deliveryAddress) {
        return OrderResponse.DeliveryAddressResponse.builder()
                .receiverName(deliveryAddress.getReceiverName())
                .receiverPhone(deliveryAddress.getReceiverPhone())
                .addressLine1(deliveryAddress.getAddressLine1())
                .ward(deliveryAddress.getWard())
                .district(deliveryAddress.getDistrict())
                .city(deliveryAddress.getCity())
                .fullAddress(deliveryAddress.getFullAddress())
                .build();
    }

    private OrderResponse.OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        return OrderResponse.OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .productName(orderItem.getProductName())
                .unitPrice(orderItem.getUnitPrice())
                .quantity(orderItem.getQuantity())
                .lineTotal(orderItem.getLineTotal())
                .build();
    }
}
