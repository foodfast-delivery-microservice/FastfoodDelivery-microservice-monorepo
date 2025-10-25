package com.example.payment.interfaces.rest;

import com.example.payment.application.usecase.CreatePaymentUseCase;
import com.example.payment.application.usecase.GetPaymentStatusUseCase;
import com.example.payment.interfaces.rest.dto.CreatePaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetPaymentStatusUseCase getPaymentStatusUseCase;

    @PostMapping("/vnpay/create")
    public ResponseEntity<String> create(@Valid @RequestBody CreatePaymentRequest request) {
        String url = createPaymentUseCase.execute(request.getOrderId(), request.getUserId(), request.getAmount());
        return ResponseEntity.ok(url);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<String> status(@PathVariable Long orderId) {
        return ResponseEntity.ok(getPaymentStatusUseCase.execute(orderId).name());
    }
}



