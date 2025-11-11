package com.example.payment.domain.model;

public enum IdempotencyStatus {
    PROCESSING, // Đang xử lý
    COMPLETED,  // Đã xong
    FAILED      // Bị lỗi
}
