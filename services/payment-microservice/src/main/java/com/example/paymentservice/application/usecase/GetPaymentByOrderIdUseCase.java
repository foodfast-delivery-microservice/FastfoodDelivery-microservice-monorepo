package com.example.paymentservice.application.usecase;

import com.example.paymentservice.application.dto.PaymentResponse;
import com.example.paymentservice.domain.entities.Payment;
import com.example.paymentservice.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class GetPaymentByOrderIdUseCase {

    private final PaymentRepository paymentRepository;

    public Optional<PaymentResponse> execute(Long orderId) {
        log.info("Getting payment for orderId: {}", orderId);

        return paymentRepository.findByOrderId(orderId)
                .map(this::toResponse);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .merchantId(payment.getMerchantId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().toString())
                .transactionNo(payment.getTransactionNo())
                .failReason(payment.getFailReason())
                .timestamp(payment.getCreatedAt())
                .build();
    }
}

