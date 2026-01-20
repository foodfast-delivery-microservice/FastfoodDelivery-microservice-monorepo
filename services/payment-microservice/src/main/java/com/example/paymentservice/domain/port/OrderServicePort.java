package com.example.paymentservice.domain.port;

import com.example.paymentservice.application.dto.OrderDetailResponse;

/**
 * Port interface for Order Service integration
 * Used to validate order ownership and status
 */
public interface OrderServicePort {
    /**
     * Get order details by order ID
     * @param orderId Order ID
     * @return OrderDetailResponse with order information
     */
    OrderDetailResponse getOrderDetail(Long orderId);
}

