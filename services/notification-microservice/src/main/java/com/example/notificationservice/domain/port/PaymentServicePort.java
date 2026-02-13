package com.example.notificationservice.domain.port;

import com.example.notificationservice.application.dto.PaymentDetailResponse;

public interface PaymentServicePort {
    PaymentDetailResponse getPaymentByOrderId(Long orderId);
}
