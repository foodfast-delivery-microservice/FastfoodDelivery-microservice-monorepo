package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.OrderListRequest;
import com.example.order_service.application.dto.OrderListResponse;
import com.example.order_service.application.dto.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderListUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private GetOrderListUseCase getOrderListUseCase;

    private Order testOrder;
    private OrderListRequest request;

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

        request = OrderListRequest.builder()
                .userId(1L)
                .page(0)
                .size(20)
                .sortBy("createdAt")
                .sortDirection("DESC")
                .build();
    }

    @Test
    void execute_ShouldReturnOrderList_WhenValidRequest() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 20), 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(orderPage);

        // When
        PageResponse<OrderListResponse> result = getOrderListUseCase.execute(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        assertFalse(result.hasNext());
        assertFalse(result.hasPrevious());

        OrderListResponse orderResponse = result.getContent().get(0);
        assertEquals(testOrder.getId(), orderResponse.getId());
        assertEquals(testOrder.getOrderCode(), orderResponse.getOrderCode());
        assertEquals(testOrder.getUserId(), orderResponse.getUserId());
        assertEquals(testOrder.getStatus().name(), orderResponse.getStatus());
        assertEquals(testOrder.getCurrency(), orderResponse.getCurrency());
        assertEquals(testOrder.getSubtotal(), orderResponse.getSubtotal());
        assertEquals(testOrder.getDiscount(), orderResponse.getDiscount());
        assertEquals(testOrder.getShippingFee(), orderResponse.getShippingFee());
        assertEquals(testOrder.getGrandTotal(), orderResponse.getGrandTotal());
        assertEquals(testOrder.getNote(), orderResponse.getNote());
        assertEquals(testOrder.getCreatedAt(), orderResponse.getCreatedAt());
        assertEquals(testOrder.getDeliveryAddress().getReceiverName(), orderResponse.getReceiverName());
        assertEquals(testOrder.getDeliveryAddress().getReceiverPhone(), orderResponse.getReceiverPhone());
        assertEquals(testOrder.getDeliveryAddress().getFullAddress(), orderResponse.getFullAddress());
        assertEquals(testOrder.getOrderItems().size(), orderResponse.getItemCount());
    }

    @Test
    void execute_ShouldReturnEmptyList_WhenNoOrdersFound() {
        // Given
        Page<Order> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 20), 0);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        PageResponse<OrderListResponse> result = getOrderListUseCase.execute(request);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        assertFalse(result.hasNext());
        assertFalse(result.hasPrevious());
    }

    @Test
    void execute_ShouldFilterByUserId_WhenUserIdProvided() {
        // Given
        request.setUserId(1L);
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 20), 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(orderPage);

        // When
        PageResponse<OrderListResponse> result = getOrderListUseCase.execute(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getUserId());
    }

    @Test
    void execute_ShouldFilterByStatus_WhenStatusProvided() {
        // Given
        request.setStatus("PENDING");
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 20), 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(orderPage);

        // When
        PageResponse<OrderListResponse> result = getOrderListUseCase.execute(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("PENDING", result.getContent().get(0).getStatus());
    }

    @Test
    void execute_ShouldFilterByOrderCode_WhenOrderCodeProvided() {
        // Given
        request.setOrderCode("ORD123");
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 20), 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(orderPage);

        // When
        PageResponse<OrderListResponse> result = getOrderListUseCase.execute(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getOrderCode().contains("ORD123"));
    }
}
