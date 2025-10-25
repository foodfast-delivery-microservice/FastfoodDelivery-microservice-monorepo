package com.example.payment.application.usecase;

import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPaymentStatusUseCase {

    private final PaymentRepository paymentRepository;

    public Payment.Status execute(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(Payment::getStatus)
                .orElse(Payment.Status.FAILED);
    }
}



