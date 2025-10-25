package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.OrderDetailResponse;
import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.model.DeliveryAddress;
import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.model.OrderItem;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderDetailUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private GetOrderDetailUseCase getOrderDetailUseCase;

    private Order testOrder;

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
    }

    @Test
    void execute_ShouldReturnOrderDetail_WhenOrderExists() {
        // Given
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When
        OrderDetailResponse result = getOrderDetailUseCase.execute(orderId);

        // Then
        assertNotNull(result);
        assertEquals(testOrder.getId(), result.getId());
        assertEquals(testOrder.getOrderCode(), result.getOrderCode());
        assertEquals(testOrder.getUserId(), result.getUserId());
        assertEquals(testOrder.getStatus().name(), result.getStatus());
        assertEquals(testOrder.getCurrency(), result.getCurrency());
        assertEquals(testOrder.getSubtotal(), result.getSubtotal());
        assertEquals(testOrder.getDiscount(), result.getDiscount());
        assertEquals(testOrder.getShippingFee(), result.getShippingFee());
        assertEquals(testOrder.getGrandTotal(), result.getGrandTotal());
        assertEquals(testOrder.getNote(), result.getNote());
        assertEquals(testOrder.getCreatedAt(), result.getCreatedAt());

        // Check delivery address
        assertNotNull(result.getDeliveryAddress());
        assertEquals(testOrder.getDeliveryAddress().getReceiverName(), result.getDeliveryAddress().getReceiverName());
        assertEquals(testOrder.getDeliveryAddress().getReceiverPhone(), result.getDeliveryAddress().getReceiverPhone());
        assertEquals(testOrder.getDeliveryAddress().getAddressLine1(), result.getDeliveryAddress().getAddressLine1());
        assertEquals(testOrder.getDeliveryAddress().getWard(), result.getDeliveryAddress().getWard());
        assertEquals(testOrder.getDeliveryAddress().getDistrict(), result.getDeliveryAddress().getDistrict());
        assertEquals(testOrder.getDeliveryAddress().getCity(), result.getDeliveryAddress().getCity());
        assertEquals(testOrder.getDeliveryAddress().getFullAddress(), result.getDeliveryAddress().getFullAddress());

        // Check order items
        assertNotNull(result.getOrderItems());
        assertEquals(1, result.getOrderItems().size());
        assertEquals(testOrder.getOrderItems().get(0).getId(), result.getOrderItems().get(0).getId());
        assertEquals(testOrder.getOrderItems().get(0).getProductId(), result.getOrderItems().get(0).getProductId());
        assertEquals(testOrder.getOrderItems().get(0).getProductName(), result.getOrderItems().get(0).getProductName());
        assertEquals(testOrder.getOrderItems().get(0).getUnitPrice(), result.getOrderItems().get(0).getUnitPrice());
        assertEquals(testOrder.getOrderItems().get(0).getQuantity(), result.getOrderItems().get(0).getQuantity());
        assertEquals(testOrder.getOrderItems().get(0).getLineTotal(), result.getOrderItems().get(0).getLineTotal());
    }

    @Test
    void execute_ShouldThrowOrderNotFoundException_WhenOrderNotFound() {
        // Given
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> {
            getOrderDetailUseCase.execute(orderId);
        });

        assertEquals("Order not found with id: 999", exception.getMessage());
    }

    @Test
    void execute_ShouldReturnOrderWithMultipleItems_WhenOrderHasMultipleItems() {
        // Given
        OrderItem orderItem2 = OrderItem.builder()
                .id(2L)
                .productId("PROD002")
                .productName("Test Product 2")
                .unitPrice(new BigDecimal("50000"))
                .quantity(1)
                .lineTotal(new BigDecimal("50000"))
                .build();

        testOrder.setOrderItems(Arrays.asList(testOrder.getOrderItems().get(0), orderItem2));
        testOrder.calculateTotals();

        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When
        OrderDetailResponse result = getOrderDetailUseCase.execute(orderId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getOrderItems().size());
        assertEquals(new BigDecimal("250000"), result.getSubtotal());
        assertEquals(new BigDecimal("260000"), result.getGrandTotal());
    }
}
