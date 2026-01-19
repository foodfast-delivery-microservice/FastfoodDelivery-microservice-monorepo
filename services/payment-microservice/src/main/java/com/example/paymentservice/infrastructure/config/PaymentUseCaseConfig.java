package com.example.paymentservice.infrastructure.config;

import com.example.paymentservice.application.usecase.ProcessPaymentUseCase;
import com.example.paymentservice.application.usecase.ProcessRefundUseCase;
import com.example.paymentservice.domain.port.OrderServicePort;
import com.example.paymentservice.domain.port.UserServicePort;
import com.example.paymentservice.domain.repository.OutboxEventRepository;
import com.example.paymentservice.domain.repository.PaymentRepository;
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
    private final OrderServicePort orderServicePort;
    private final UserServicePort userServicePort;

    @Bean
    public ProcessPaymentUseCase processPaymentUseCase() {
        return new ProcessPaymentUseCase(
                paymentRepository, 
                outboxEventRepository, 
                objectMapper,
                orderServicePort,
                userServicePort
        );
    }
    @Bean
    public ProcessRefundUseCase processRefundUseCase() {
        return new ProcessRefundUseCase(paymentRepository, outboxEventRepository, objectMapper);
    }
}
