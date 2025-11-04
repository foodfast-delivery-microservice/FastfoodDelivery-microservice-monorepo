package com.example.payment.infrastructure.config;

import com.example.payment.application.usecase.ProcessPaymentUseCase;
import com.example.payment.domain.repository.OutboxEventRepository;
import com.example.payment.domain.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class PaymentUseCaseConfig {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Bean
    public ProcessPaymentUseCase processPaymentUseCase() {
        return new ProcessPaymentUseCase(paymentRepository, outboxEventRepository, objectMapper);
    }
}
