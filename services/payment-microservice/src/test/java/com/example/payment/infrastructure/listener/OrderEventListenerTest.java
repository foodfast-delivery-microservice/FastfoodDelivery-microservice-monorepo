package com.example.payment.infrastructure.listener;

import com.example.payment.application.dto.PaymentRequest;
import com.example.payment.application.usecase.ProcessPaymentUseCase;
import com.example.payment.infrastructure.lisener.OrderEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
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
    private ObjectMapper objectMapper;

    @Mock
    private ProcessPaymentUseCase processPaymentUseCase;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private String validOrderCreatedJson;
    private PaymentRequest validPaymentRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Setup valid order created event (includes merchantId from OrderCreatedEventPayload)
        validOrderCreatedJson = "{\"orderId\":1,\"userId\":1,\"merchantId\":10,\"grandTotal\":100000,\"currency\":\"VND\"}";
        
        validPaymentRequest = new PaymentRequest(
                1L,  // orderId
                1L,  // userId
                10L,  // merchantId (from event payload)
                new BigDecimal("100000"),  // grandTotal
                "VND"  // currency
        );
    }

    @Test
    @DisplayName("Test 2.1: Consume order.created event successfully")
    void testConsumeOrderCreatedEvent_Success() throws Exception {
        // Given: Valid order.created event
        when(objectMapper.readValue(validOrderCreatedJson, PaymentRequest.class))
                .thenReturn(validPaymentRequest);
        doNothing().when(processPaymentUseCase).createPayment(validPaymentRequest);

        // When: Receive event from RabbitMQ
        orderEventListener.handleOrderCreatedEvent(validOrderCreatedJson);

        // Then: Payment record được tạo
        verify(processPaymentUseCase, times(1)).createPayment(validPaymentRequest);
        verify(objectMapper, times(1)).readValue(validOrderCreatedJson, PaymentRequest.class);
    }

    @Test
    @DisplayName("Test 2.2: Handle invalid JSON message")
    void testConsumeOrderCreatedEvent_InvalidJson() throws Exception {
        // Given: Invalid JSON message
        String invalidJson = "invalid json";
        when(objectMapper.readValue(invalidJson, PaymentRequest.class))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Invalid JSON") {});

        // When: Receive invalid event
        // Then: Exception được throw
        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> orderEventListener.handleOrderCreatedEvent(invalidJson)
        );

        // Verify: createPayment không được gọi
        verify(processPaymentUseCase, never()).createPayment(any());
    }

    @Test
    @DisplayName("Test 2.3: Handle processing exception")
    void testConsumeOrderCreatedEvent_ProcessingException() throws Exception {
        // Given: Valid event nhưng processing lỗi
        when(objectMapper.readValue(validOrderCreatedJson, PaymentRequest.class))
                .thenReturn(validPaymentRequest);
        doThrow(new RuntimeException("Database error"))
                .when(processPaymentUseCase).createPayment(validPaymentRequest);

        // When: Receive event
        // Then: Exception được throw
        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> orderEventListener.handleOrderCreatedEvent(validOrderCreatedJson)
        );

        // Verify: createPayment được gọi nhưng fail
        verify(processPaymentUseCase, times(1)).createPayment(validPaymentRequest);
    }

    @Test
    @DisplayName("Test 2.4: Handle null message")
    void testConsumeOrderCreatedEvent_NullMessage() {
        // Given: Null message
        // When: Receive null event
        // Then: Exception được throw
        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> orderEventListener.handleOrderCreatedEvent(null)
        );
    }

    @Test
    @DisplayName("Test 2.5: Handle empty message")
    void testConsumeOrderCreatedEvent_EmptyMessage() throws Exception {
        // Given: Empty message
        String emptyJson = "{}";
        when(objectMapper.readValue(emptyJson, PaymentRequest.class))
                .thenReturn(new PaymentRequest(null, null, null, null, null));
        doNothing().when(processPaymentUseCase).createPayment(any(PaymentRequest.class));

        // When: Receive empty event
        orderEventListener.handleOrderCreatedEvent(emptyJson);

        // Then: createPayment được gọi với empty request
        verify(processPaymentUseCase, times(1)).createPayment(any(PaymentRequest.class));
    }
}

