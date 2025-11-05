package com.example.payment.interfaces.rest;

import com.example.payment.application.dto.PaymentRequest;
import com.example.payment.application.usecase.ProcessPaymentUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;

    @PostMapping
    public ResponseEntity<?> processPayment(@Valid @RequestBody PaymentRequest request) {
        boolean success = processPaymentUseCase.execute(request);
        return success
                ? ResponseEntity.ok("Payment processed successfully")
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment failed");
    }
}
