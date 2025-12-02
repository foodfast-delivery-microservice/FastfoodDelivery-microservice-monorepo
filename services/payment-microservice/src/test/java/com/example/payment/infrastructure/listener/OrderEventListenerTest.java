package com.example.payment.infrastructure.listener;

import com.example.payment.application.dto.PaymentRequest;
import com.example.payment.application.usecase.ProcessPaymentUseCase;
import com.example.payment.infrastructure.lisener.OrderEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

/**
 * Unit Tests cho OrderEventListener (RabbitMQ Consumer)
 * 
 * Test Scenarios:
 * 1. Consume order.created event successfully
 * 2. Handle invalid JSON message
 * 3. Handle processing exception
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventListener RabbitMQ Tests")
class OrderEventListenerTest {

    @Mock
    private ProcessPaymentUseCase processPaymentUseCase;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private PaymentRequest validPaymentRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Setup valid order created event (includes merchantId from OrderCreatedEventPayload)
        validPaymentRequest = new PaymentRequest(
                1L,  // orderId
                1L,  // userId
                10L,  // merchantId (from event payload)
                new BigDecimal("100000"),  // grandTotal
                "VND"  // currency
        );
    }

    @Test
    @DisplayName("Test 2.1: Consume order.created event successfully (PaymentRequest delivered by converter)")
    void testConsumeOrderCreatedEvent_Success() {
        // Given: Valid PaymentRequest (đã được Jackson convert từ JSON trước đó)
        doNothing().when(processPaymentUseCase).createPayment(validPaymentRequest);

        // When: Listener nhận được PaymentRequest
        orderEventListener.handleOrderCreatedEvent(validPaymentRequest);

        // Then: Payment record được tạo
        verify(processPaymentUseCase, times(1)).createPayment(validPaymentRequest);
    }

    @Test
    @DisplayName("Test 2.2: Handle processing exception")
    void testConsumeOrderCreatedEvent_ProcessingException() {
        // Given: Valid PaymentRequest nhưng processing lỗi
        doThrow(new RuntimeException("Database error"))
                .when(processPaymentUseCase).createPayment(validPaymentRequest);

        // When: Receive event
        // Then: Exception được throw
        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> orderEventListener.handleOrderCreatedEvent(validPaymentRequest)
        );

        // Verify: createPayment được gọi nhưng fail
        verify(processPaymentUseCase, times(1)).createPayment(validPaymentRequest);
    }
}

