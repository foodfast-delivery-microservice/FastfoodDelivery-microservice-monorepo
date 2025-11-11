package com.example.order_service.interfaces.rest;

import com.example.order_service.application.dto.CreateOrderRequest;
import com.example.order_service.domain.model.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void contextLoads() {
        // This test will pass if the Spring context loads successfully
    }

    @Test
    void testCreateOrderEndpoint() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(1L)
                .orderItems(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("John Doe")
                        .receiverPhone("0901234567")
                        .addressLine1("123 Test Street")
                        .ward("Test Ward")
                        .district("Test District")
                        .city("Test City")
                        .build())
                .build();

        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "test-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING.toString()));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/orders/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Order Service is running"));
    }

    // =====================================================================
    // INTEGRATION TESTS: DELIVERY ADDRESS VALIDATION
    // =====================================================================

    @Test
    void testCreateOrder_InvalidPhoneFormat() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(1L)
                .orderItems(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("John Doe")
                        .receiverPhone("123456789") // Invalid: missing leading 0
                        .addressLine1("123 Test Street")
                        .ward("Test Ward")
                        .district("Test District")
                        .city("Test City")
                        .build())
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_InvalidPhoneLength() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(1L)
                .orderItems(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("John Doe")
                        .receiverPhone("09012345") // Invalid: only 9 digits
                        .addressLine1("123 Test Street")
                        .ward("Test Ward")
                        .district("Test District")
                        .city("Test City")
                        .build())
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_EmptyReceiverName() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(1L)
                .orderItems(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("") // Invalid: empty
                        .receiverPhone("0901234567")
                        .addressLine1("123 Test Street")
                        .ward("Test Ward")
                        .district("Test District")
                        .city("Test City")
                        .build())
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_ReceiverNameTooShort() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(1L)
                .orderItems(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("A") // Invalid: too short
                        .receiverPhone("0901234567")
                        .addressLine1("123 Test Street")
                        .ward("Test Ward")
                        .district("Test District")
                        .city("Test City")
                        .build())
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_AddressLine1TooShort() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(1L)
                .orderItems(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("John Doe")
                        .receiverPhone("0901234567")
                        .addressLine1("123") // Invalid: too short
                        .ward("Test Ward")
                        .district("Test District")
                        .city("Test City")
                        .build())
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_WardTooShort() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(1L)
                .orderItems(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("John Doe")
                        .receiverPhone("0901234567")
                        .addressLine1("123 Test Street")
                        .ward("W") // Invalid: too short
                        .district("Test District")
                        .city("Test City")
                        .build())
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
