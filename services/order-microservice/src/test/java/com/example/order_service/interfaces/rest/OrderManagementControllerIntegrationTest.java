package com.example.order_service.interfaces.rest;

import com.example.order_service.application.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderManagementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String orderIdempotencyKey;
    private Long createdOrderId;

    @BeforeEach
    void setUp() throws Exception {
        // Create a test order first
        orderIdempotencyKey = "test-order-management-" + System.currentTimeMillis();
        createdOrderId = createTestOrder();
    }

    private Long createTestOrder() throws Exception {
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .userId(1L)
                .orderItems(Arrays.asList(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId("PROD001")
                                .productName("Test Product")
                                .unitPrice(new BigDecimal("100000"))
                                .quantity(2)
                                .build()
                ))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("John Doe")
                        .receiverPhone("0123456789")
                        .addressLine1("123 Test Street")
                        .ward("Test Ward")
                        .district("Test District")
                        .city("Test City")
                        .build())
                .discount(BigDecimal.ZERO)
                .shippingFee(new BigDecimal("10000"))
                .note("Test order for management")
                .build();

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", orderIdempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        com.example.order_service.application.dto.OrderResponse orderResponse =
                objectMapper.readValue(responseContent, com.example.order_service.application.dto.OrderResponse.class);

        return orderResponse.getId();
    }

    @Test
    void getOrderList_ShouldReturnOrderList_WhenValidRequest() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/api/orders")
                        .param("userId", "1")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        PageResponse<OrderListResponse> pageResponse = objectMapper.readValue(
                responseContent,
                objectMapper.getTypeFactory().constructParametricType(PageResponse.class, OrderListResponse.class)
        );

        assertNotNull(pageResponse);
        assertTrue(pageResponse.getContent().size() > 0);
        assertTrue(pageResponse.getTotalElements() > 0);
        assertTrue(pageResponse.isFirst());
        assertFalse(pageResponse.getContent().isEmpty());
    }

    @Test
    void getOrderList_ShouldFilterByStatus_WhenStatusProvided() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/api/orders")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        PageResponse<OrderListResponse> pageResponse = objectMapper.readValue(
                responseContent,
                objectMapper.getTypeFactory().constructParametricType(PageResponse.class, OrderListResponse.class)
        );

        assertNotNull(pageResponse);
        pageResponse.getContent().forEach(order ->
                assertEquals("PENDING", order.getStatus())
        );
    }

    @Test
    void getOrderDetail_ShouldReturnOrderDetail_WhenOrderExists() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/api/orders/{orderId}", createdOrderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        OrderDetailResponse orderDetail = objectMapper.readValue(responseContent, OrderDetailResponse.class);

        assertNotNull(orderDetail);
        assertEquals(createdOrderId, orderDetail.getId());
        assertEquals("PENDING", orderDetail.getStatus());
        assertEquals(1L, orderDetail.getUserId());
        assertNotNull(orderDetail.getDeliveryAddress());
        assertNotNull(orderDetail.getOrderItems());
        assertFalse(orderDetail.getOrderItems().isEmpty());
    }

    @Test
    void getOrderDetail_ShouldReturnNotFound_WhenOrderNotExists() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/orders/{orderId}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOrderStatus_ShouldUpdateStatus_WhenValidTransition() throws Exception {
        // Given
        UpdateOrderStatusRequest updateRequest = UpdateOrderStatusRequest.builder()
                .status("CONFIRMED")
                .note("Order confirmed by admin")
                .build();

        // When
        MvcResult result = mockMvc.perform(put("/api/orders/{orderId}/status", createdOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        OrderDetailResponse orderDetail = objectMapper.readValue(responseContent, OrderDetailResponse.class);

        assertNotNull(orderDetail);
        assertEquals(createdOrderId, orderDetail.getId());
        assertEquals("CONFIRMED", orderDetail.getStatus());
    }

    @Test
    void updateOrderStatus_ShouldReturnBadRequest_WhenInvalidTransition() throws Exception {
        // Given - First confirm the order
        UpdateOrderStatusRequest confirmRequest = UpdateOrderStatusRequest.builder()
                .status("CONFIRMED")
                .note("Order confirmed")
                .build();

        mockMvc.perform(put("/api/orders/{orderId}/status", createdOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());

        // Then try to cancel confirmed order (invalid transition)
        UpdateOrderStatusRequest cancelRequest = UpdateOrderStatusRequest.builder()
                .status("CANCELLED")
                .note("Trying to cancel confirmed order")
                .build();

        mockMvc.perform(put("/api/orders/{orderId}/status", createdOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrderStatus_ShouldReturnNotFound_WhenOrderNotExists() throws Exception {
        // Given
        UpdateOrderStatusRequest updateRequest = UpdateOrderStatusRequest.builder()
                .status("CONFIRMED")
                .note("Order confirmed")
                .build();

        // When & Then
        mockMvc.perform(put("/api/orders/{orderId}/status", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchOrders_ShouldReturnFilteredResults_WhenKeywordProvided() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/api/orders/search")
                        .param("keyword", "ORD")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        PageResponse<OrderListResponse> pageResponse = objectMapper.readValue(
                responseContent,
                objectMapper.getTypeFactory().constructParametricType(PageResponse.class, OrderListResponse.class)
        );

        assertNotNull(pageResponse);
        pageResponse.getContent().forEach(order ->
                assertTrue(order.getOrderCode().contains("ORD"))
        );
    }

    @Test
    void searchOrders_ShouldReturnEmptyResults_WhenNoMatchingKeyword() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/api/orders/search")
                        .param("keyword", "NONEXISTENT")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        PageResponse<OrderListResponse> pageResponse = objectMapper.readValue(
                responseContent,
                objectMapper.getTypeFactory().constructParametricType(PageResponse.class, OrderListResponse.class)
        );

        assertNotNull(pageResponse);
        assertTrue(pageResponse.getContent().isEmpty());
        assertEquals(0, pageResponse.getTotalElements());
    }
}
