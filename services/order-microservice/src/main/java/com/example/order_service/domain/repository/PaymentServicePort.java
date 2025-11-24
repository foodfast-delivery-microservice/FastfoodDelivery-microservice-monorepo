package com.example.order_service.domain.repository;

import com.example.order_service.application.dto.PaymentInfo;
import com.example.order_service.application.dto.PaymentValidationResponse;

/**
 * Port interface for Payment Service integration
 * Follows the same pattern as ProductServicePort and UserServicePort
 */
public interface PaymentServicePort {
    /**
     * Validate payment method is valid for user
     * @param userId User ID
     * @param paymentMethod Payment method to validate (e.g., "COD", "CARD", "WALLET")
     * @return PaymentValidationResponse with validation result
     */
    PaymentValidationResponse validatePaymentMethod(Long userId, String paymentMethod);

    /**
     * Get payment information by orderId
     * @param orderId Order ID
     * @return PaymentInfo with paymentId, orderId, amount, status
     * @throws RuntimeException if payment not found or service unavailable
     */
    PaymentInfo getPaymentByOrderId(Long orderId);
}

