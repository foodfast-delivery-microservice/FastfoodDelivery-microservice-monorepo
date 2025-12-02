package com.example.order_service.domain.model;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAID,
    PROCESSING,  // Order đang được xử lý (chuẩn bị hàng)
    SHIPPED,      // Order đã được gửi đi (truyền thống)
    DELIVERING,   // Order đang được giao (drone đang giao hàng)
    DELIVERED,
    CANCELLED,
    REFUNDED
}
