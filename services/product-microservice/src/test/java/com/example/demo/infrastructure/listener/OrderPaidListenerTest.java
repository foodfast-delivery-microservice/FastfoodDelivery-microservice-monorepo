package com.example.demo.infrastructure.listener;

import com.example.demo.application.usecase.DeductStockUseCase;
import com.example.demo.domain.model.StockDeductionRecord;
import com.example.demo.domain.repository.StockDeductionRecordRepository;
import com.example.demo.infrastructure.event.OrderPaidEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho OrderPaidListener
 * Mục đích: Test logic xử lý OrderPaidEvent và idempotency
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderPaidListener Unit Tests")
class OrderPaidListenerTest {

    @Mock
    private DeductStockUseCase deductStockUseCase;

    @Mock
    private StockDeductionRecordRepository stockDeductionRecordRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderPaidListener orderPaidListener;

    private OrderPaidEvent validEvent;
    private String validJsonPayload;
    private Long orderId;
    private Long merchantId;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        orderId = 100L;
        merchantId = 10L;

        List<OrderPaidEvent.OrderItemDeduction> orderItems = List.of(
                OrderPaidEvent.OrderItemDeduction.builder()
                        .productId(1L)
                        .quantity(5)
                        .merchantId(merchantId)
                        .build(),
                OrderPaidEvent.OrderItemDeduction.builder()
                        .productId(2L)
                        .quantity(3)
                        .merchantId(merchantId)
                        .build()
        );

        validEvent = OrderPaidEvent.builder()
                .orderId(orderId)
                .userId(1L)
                .merchantId(merchantId)
                .orderItems(orderItems)
                .build();

        validJsonPayload = "{\"orderId\":100,\"userId\":1,\"merchantId\":10,\"orderItems\":[{\"productId\":1,\"quantity\":5,\"merchantId\":10},{\"productId\":2,\"quantity\":3,\"merchantId\":10}]}";

        lenient().when(objectMapper.readValue(validJsonPayload, OrderPaidEvent.class))
                .thenReturn(validEvent);
    }

    @Test
    @DisplayName("✅ Should process event successfully, deduct stock and save idempotency record")
    void testHandleOrderPaid_Success() throws Exception {
        // Given
        when(stockDeductionRecordRepository.existsByOrderId(orderId))
                .thenReturn(false);

        // When
        orderPaidListener.handleOrderPaid(validJsonPayload);

        // Then
        verify(deductStockUseCase, times(1)).execute(1L, 5, merchantId);
        verify(deductStockUseCase, times(1)).execute(2L, 3, merchantId);

        ArgumentCaptor<StockDeductionRecord> recordCaptor = ArgumentCaptor.forClass(StockDeductionRecord.class);
        verify(stockDeductionRecordRepository, times(1)).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("✅ Should skip processing when duplicate event received (idempotency check)")
    void testHandleOrderPaid_DuplicateEvent() throws Exception {
        // Given
        when(stockDeductionRecordRepository.existsByOrderId(orderId))
                .thenReturn(true);

        // When
        orderPaidListener.handleOrderPaid(validJsonPayload);

        // Then
        verify(deductStockUseCase, never()).execute(any(), any(), any());
        verify(stockDeductionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("✅ Should return early when event has no order items")
    void testHandleOrderPaid_EmptyOrderItems() throws Exception {
        // Given
        OrderPaidEvent emptyEvent = OrderPaidEvent.builder()
                .orderId(orderId)
                .userId(1L)
                .merchantId(merchantId)
                .orderItems(new ArrayList<>())
                .build();
        String emptyJsonPayload = "{\"orderId\":100,\"userId\":1,\"merchantId\":10,\"orderItems\":[]}";

        when(objectMapper.readValue(emptyJsonPayload, OrderPaidEvent.class))
                .thenReturn(emptyEvent);
        when(stockDeductionRecordRepository.existsByOrderId(orderId))
                .thenReturn(false);

        // When
        orderPaidListener.handleOrderPaid(emptyJsonPayload);

        // Then
        verify(deductStockUseCase, never()).execute(any(), any(), any());
        verify(stockDeductionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("✅ Should return early when event has null order items")
    void testHandleOrderPaid_NullOrderItems() throws Exception {
        // Given
        OrderPaidEvent nullItemsEvent = OrderPaidEvent.builder()
                .orderId(orderId)
                .userId(1L)
                .merchantId(merchantId)
                .orderItems(null)
                .build();
        String nullItemsJsonPayload = "{\"orderId\":100,\"userId\":1,\"merchantId\":10,\"orderItems\":null}";

        when(objectMapper.readValue(nullItemsJsonPayload, OrderPaidEvent.class))
                .thenReturn(nullItemsEvent);
        when(stockDeductionRecordRepository.existsByOrderId(orderId))
                .thenReturn(false);

        // When
        orderPaidListener.handleOrderPaid(nullItemsJsonPayload);

        // Then
        verify(deductStockUseCase, never()).execute(any(), any(), any());
        verify(stockDeductionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Should throw AmqpRejectAndDontRequeueException when business error occurs (insufficient stock)")
    void testHandleOrderPaid_BusinessError() throws Exception {
        // Given
        when(stockDeductionRecordRepository.existsByOrderId(orderId))
                .thenReturn(false);
        doThrow(new IllegalArgumentException("Insufficient stock"))
                .when(deductStockUseCase).execute(1L, 5, merchantId);

        // When & Then
        assertThatThrownBy(() -> orderPaidListener.handleOrderPaid(validJsonPayload))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("Business validation failed");

        verify(deductStockUseCase, times(1)).execute(1L, 5, merchantId);
        verify(deductStockUseCase, never()).execute(2L, 3, merchantId);
        verify(stockDeductionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Should throw exception when technical error occurs (for retry)")
    void testHandleOrderPaid_TechnicalError() throws Exception {
        // Given
        when(stockDeductionRecordRepository.existsByOrderId(orderId))
                .thenReturn(false);
        doThrow(new RuntimeException("Database connection error"))
                .when(deductStockUseCase).execute(1L, 5, merchantId);

        // When & Then
        assertThatThrownBy(() -> orderPaidListener.handleOrderPaid(validJsonPayload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection error");

        verify(deductStockUseCase, times(1)).execute(1L, 5, merchantId);
        verify(stockDeductionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Should throw AmqpRejectAndDontRequeueException when invalid JSON payload")
    void testHandleOrderPaid_InvalidJson() throws Exception {
        // Given
        String invalidJson = "invalid json";
        when(objectMapper.readValue(invalidJson, OrderPaidEvent.class))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});

        // When & Then
        assertThatThrownBy(() -> orderPaidListener.handleOrderPaid(invalidJson))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("Failed to parse event payload");

        verify(deductStockUseCase, never()).execute(any(), any(), any());
        verify(stockDeductionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("✅ Should save idempotency record AFTER all items deducted successfully")
    void testHandleOrderPaid_IdempotencyRecordSavedAfterDeduction() throws Exception {
        // Given
        when(stockDeductionRecordRepository.existsByOrderId(orderId))
                .thenReturn(false);

        // When
        orderPaidListener.handleOrderPaid(validJsonPayload);

        // Then - Verify order of operations using InOrder
        var inOrder = inOrder(deductStockUseCase, stockDeductionRecordRepository);
        inOrder.verify(deductStockUseCase).execute(1L, 5, merchantId);
        inOrder.verify(deductStockUseCase).execute(2L, 3, merchantId);
        inOrder.verify(stockDeductionRecordRepository).save(any(StockDeductionRecord.class));
    }

    @Test
    @DisplayName("❌ Should not save idempotency record when second item fails")
    void testHandleOrderPaid_NoIdempotencyRecordWhenPartialFailure() throws Exception {
        // Given
        when(stockDeductionRecordRepository.existsByOrderId(orderId))
                .thenReturn(false);
        doNothing().when(deductStockUseCase).execute(1L, 5, merchantId);
        doThrow(new IllegalArgumentException("Insufficient stock"))
                .when(deductStockUseCase).execute(2L, 3, merchantId);

        // When & Then
        assertThatThrownBy(() -> orderPaidListener.handleOrderPaid(validJsonPayload))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(deductStockUseCase, times(1)).execute(1L, 5, merchantId);
        verify(deductStockUseCase, times(1)).execute(2L, 3, merchantId);
        verify(stockDeductionRecordRepository, never()).save(any());
    }
}



