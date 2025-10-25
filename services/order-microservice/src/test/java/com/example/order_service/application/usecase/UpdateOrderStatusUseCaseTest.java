package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.OrderDetailResponse;
import com.example.order_service.application.dto.UpdateOrderStatusRequest;
import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.model.*;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateOrderStatusUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    private Order testOrder;
    private UpdateOrderStatusRequest request;

    @BeforeEach
    void setUp() {
        // Create test order
        DeliveryAddress deliveryAddress = DeliveryAddress.builder()
                .receiverName("John Doe")
                .receiverPhone("0123456789")
                .addressLine1("123 Test Street")
                .ward("Test Ward")
                .district("Test District")
                .city("Test City")
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .productId("PROD001")
                .productName("Test Product")
                .unitPrice(new BigDecimal("100000"))
                .quantity(2)
                .lineTotal(new BigDecimal("200000"))
                .build();

        testOrder = Order.builder()
                .id(1L)
                .orderCode("ORD1234567890ABCDEFGH")
                .userId(1L)
                .status(OrderStatus.PENDING)
                .currency("VND")
                .subtotal(new BigDecimal("200000"))
                .discount(BigDecimal.ZERO)
                .shippingFee(new BigDecimal("10000"))
                .grandTotal(new BigDecimal("210000"))
                .note("Test note")
                .deliveryAddress(deliveryAddress)
                .createdAt(LocalDateTime.now())
                .orderItems(Arrays.asList(orderItem))
                .build();

        request = UpdateOrderStatusRequest.builder()
                .status("CONFIRMED")
                .note("Order confirmed")
                .build();
    }

    @Test
    void execute_ShouldUpdateOrderStatus_WhenValidTransition() {
        // Given
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        // When
        OrderDetailResponse result = updateOrderStatusUseCase.execute(orderId, request);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED.name(), result.getStatus());

        // Verify order was saved
        verify(orderRepository).save(testOrder);

        // Verify outbox event was created
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        OutboxEvent capturedEvent = eventCaptor.getValue();
        assertEquals("Order", capturedEvent.getAggregateType());
        assertEquals("1", capturedEvent.getAggregateId());
        assertEquals("OrderStatusChanged", capturedEvent.getType());
        assertEquals(EventStatus.NEW, capturedEvent.getStatus());
    }

    @Test
    void execute_ShouldThrowOrderNotFoundException_WhenOrderNotFound() {
        // Given
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> {
            updateOrderStatusUseCase.execute(orderId, request);
        });

        assertEquals("Order not found with id: 999", exception.getMessage());
    }

    @Test
    void execute_ShouldThrowValidationException_WhenStatusIsEmpty() {
        // Given
        Long orderId = 1L;
        request.setStatus("");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        OrderValidationException exception = assertThrows(OrderValidationException.class, () -> {
            updateOrderStatusUseCase.execute(orderId, request);
        });

        assertEquals("Status is required", exception.getMessage());
    }

    @Test
    void execute_ShouldThrowValidationException_WhenInvalidStatus() {
        // Given
        Long orderId = 1L;
        request.setStatus("INVALID_STATUS");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        OrderValidationException exception = assertThrows(OrderValidationException.class, () -> {
            updateOrderStatusUseCase.execute(orderId, request);
        });

        assertEquals("Invalid order status: INVALID_STATUS", exception.getMessage());
    }

    @Test
    void execute_ShouldThrowValidationException_WhenInvalidTransition() {
        // Given
        Long orderId = 1L;
        testOrder.setStatus(OrderStatus.DELIVERED); // Already delivered
        request.setStatus("CANCELLED"); // Cannot cancel delivered order
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        OrderValidationException exception = assertThrows(OrderValidationException.class, () -> {
            updateOrderStatusUseCase.execute(orderId, request);
        });

        assertTrue(exception.getMessage().contains("Cannot change order status from DELIVERED to CANCELLED"));
    }

    @Test
    void execute_ShouldUpdateToShipped_WhenFromConfirmed() {
        // Given
        Long orderId = 1L;
        testOrder.setStatus(OrderStatus.CONFIRMED);
        request.setStatus("SHIPPED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        lenient().when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        // When
        OrderDetailResponse result = updateOrderStatusUseCase.execute(orderId, request);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.SHIPPED.name(), result.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    void execute_ShouldUpdateToDelivered_WhenFromShipped() {
        // Given
        Long orderId = 1L;
        testOrder.setStatus(OrderStatus.SHIPPED);
        request.setStatus("DELIVERED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        lenient().when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        // When
        OrderDetailResponse result = updateOrderStatusUseCase.execute(orderId, request);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.DELIVERED.name(), result.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    void execute_ShouldUpdateToCancelled_WhenFromPending() {
        // Given
        Long orderId = 1L;
        request.setStatus("CANCELLED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        lenient().when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        // When
        OrderDetailResponse result = updateOrderStatusUseCase.execute(orderId, request);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED.name(), result.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    void execute_ShouldUpdateToRefunded_WhenFromDelivered() {
        // Given
        Long orderId = 1L;
        testOrder.setStatus(OrderStatus.DELIVERED);
        request.setStatus("REFUNDED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        lenient().when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        // When
        OrderDetailResponse result = updateOrderStatusUseCase.execute(orderId, request);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.REFUNDED.name(), result.getStatus());
        verify(orderRepository).save(testOrder);
    }
}
