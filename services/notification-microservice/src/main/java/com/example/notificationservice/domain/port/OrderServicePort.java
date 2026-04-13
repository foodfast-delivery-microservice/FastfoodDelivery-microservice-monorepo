package com.example.notificationservice.domain.port;

import com.example.notificationservice.application.dto.OrderDetailResponse;

public interface OrderServicePort {
    OrderDetailResponse getOrderById(Long orderId);
}
