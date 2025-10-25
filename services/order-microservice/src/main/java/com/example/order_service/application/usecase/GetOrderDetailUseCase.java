package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.DeliveryAddressResponse;
import com.example.order_service.application.dto.OrderDetailResponse;
import com.example.order_service.application.dto.OrderItemResponse;
import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetOrderDetailUseCase {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public OrderDetailResponse execute(Long orderId) {
        log.info("Getting order detail for orderId: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        return mapToOrderDetailResponse(order);
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
