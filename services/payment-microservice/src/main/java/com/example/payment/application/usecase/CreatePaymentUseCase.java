package com.example.payment.application.usecase;

import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.PaymentRepository;
import com.example.payment.infrastructure.vnpay.VNPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final VNPayService vnPayService;

    @Transactional
    public String execute(Long orderId, Long userId, BigDecimal amount) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .status(Payment.Status.PENDING)
                .build();

        String txnRef = UUID.randomUUID().toString().replace("-", "");
        payment.setVnpTxnRef(txnRef);
        payment = paymentRepository.save(payment);

        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();
        String orderInfo = "Pay for order " + orderId;
        return vnPayService.buildPaymentUrl(txnRef, vnpAmount, orderInfo);
    }
}



