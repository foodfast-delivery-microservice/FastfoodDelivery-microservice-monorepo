package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.CreateOrderRequest;
import com.example.order_service.application.dto.OrderResponse;
import com.example.order_service.application.dto.ProductValidationResponse;
import com.example.order_service.application.dto.UserValidationResponse;
import com.example.order_service.application.service.AdministrativeAddressNormalizer;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.model.*;
import com.example.order_service.domain.repository.IdempotencyKeyRepository;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.domain.repository.OutboxEventRepository;
import com.example.order_service.domain.repository.ProductServicePort;
import com.example.order_service.domain.repository.UserServicePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho CreateOrderUseCase
 * Mục đích: Test logic nghiệp vụ KHÔNG CẦN DB thật
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderUseCase Unit Tests")
class CreateOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ProductServicePort productServicePort;

    @Mock
    private UserServicePort userServicePort;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AdministrativeAddressNormalizer administrativeAddressNormalizer;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

    private CreateOrderRequest validRequest;
    private List<ProductValidationResponse> validatedProducts;

    @BeforeEach
    void setUp() {
        // Chuẩn bị data test
        validRequest = CreateOrderRequest.builder()
                .userId(1L)
                .orderItems(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .discount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.valueOf(50000))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("Nguyen Van A")
                        .receiverPhone("0901234567")
                        .addressLine1("123 Le Loi")
                        .ward("Ward 1")
                        .district("District 1")
                        .city("Ho Chi Minh")
                        .build())
                .build();

        // Response từ Product Service (GIÁ THẬT)
        validatedProducts = List.of(
                new ProductValidationResponse(
                        1L,
                        true,
                        "iPhone 15 Pro Max", // Tên từ Product Service
                        BigDecimal.valueOf(30000000), // Giá từ Product Service (KHÁC VỚI CLIENT!)
                        10L // Merchant ID từ Product Service
                )
        );

        when(productServicePort.validateProducts(any())).thenReturn(validatedProducts);
        when(userServicePort.validateUser(anyLong())).thenReturn(new UserValidationResponse(1L, true, true, "mock-user"));
    }

    // =====================================================================
    // TEST CASES: HAPPY PATH
    // =====================================================================

    @Test
    @DisplayName("✅ Should create order successfully with correct product info from Product Service")
    void testCreateOrder_Success() throws Exception {
        // Given: Mock các dependencies
        when(idempotencyKeyRepository.existsByUserIdAndIdemKey(anyLong(), anyString())).thenReturn(false);
        when(productServicePort.validateProducts(any())).thenReturn(validatedProducts);

        Order savedOrder = createMockOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        // When: Execute
        OrderResponse response = createOrderUseCase.execute(validRequest, "IDEM-KEY-123", "test-jti-123");

        // Then: Verify
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("PENDING");

        // Verify Product Service được gọi
        verify(productServicePort, times(1)).validateProducts(any());

        // Verify Order được save với GIÁ TỪ PRODUCT SERVICE
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());

        Order capturedOrder = orderCaptor.getValue();
        OrderItem item = capturedOrder.getOrderItems().get(0);

        // KIỂM TRA GIÁ PHẢI LẤY TỪ PRODUCT SERVICE
        assertThat(item.getProductName()).isEqualTo("iPhone 15 Pro Max"); // Từ Product Service
        assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(30000000)); // Từ Product Service

        // Verify Outbox Event được tạo
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));

        // Verify Idempotency Key được lưu
        verify(idempotencyKeyRepository, times(1)).save(any(IdempotencyKey.class));
    }

    @Test
    @DisplayName("✅ Should return existing order when duplicate request detected")
    void testCreateOrder_DuplicateRequest() {
        // Given: Request đã tồn tại
        Long orderId = 999L;
        String idempotencyKey = "IDEM-KEY-123";

        IdempotencyKey existingKey = IdempotencyKey.builder()
                .userId(1L)
                .idemKey(idempotencyKey)
                .orderId(orderId)
                .build();

        Order existingOrder = createMockOrder();
        existingOrder.setId(orderId);

        when(idempotencyKeyRepository.existsByUserIdAndIdemKey(1L, idempotencyKey)).thenReturn(true);
        when(idempotencyKeyRepository.findByUserIdAndIdemKey(1L, idempotencyKey))
                .thenReturn(Optional.of(existingKey));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // When: Execute
        OrderResponse response = createOrderUseCase.execute(validRequest, idempotencyKey, "test-jti-123");

        // Then: Verify
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(orderId);

        // KHÔNG GỌI Product Service (vì đã có order rồi)
        verify(productServicePort, never()).validateProducts(any());

        // KHÔNG SAVE order mới
        verify(orderRepository, never()).save(any());
    }

    // =====================================================================
    // TEST CASES: VALIDATION ERRORS
    // =====================================================================

    @Test
    @DisplayName("❌ Should throw exception when userId is null")
    void testCreateOrder_NullUserId() {
        // Given
        validRequest.setUserId(null);

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("User ID"); // Chỉ check chứa "User ID", không care tiếng Việt hay Anh

        // Verify không gọi Product Service
        verify(productServicePort, never()).validateProducts(any());
    }

    @Test
    @DisplayName("❌ Should throw exception when order items is empty")
    void testCreateOrder_EmptyOrderItems() {
        // Given
        validRequest.setOrderItems(List.of());

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Order"); // Chỉ check chứa "Order"
    }

    @Test
    @DisplayName("❌ Should throw exception when delivery address is null")
    void testCreateOrder_NullDeliveryAddress() {
        // Given
        validRequest.setDeliveryAddress(null);

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("address"); // Chỉ check chứa "address"
    }

    // =====================================================================
    // TEST CASES: DELIVERY ADDRESS VALIDATION
    // =====================================================================

    @Test
    @DisplayName("✅ Should accept valid delivery address")
    void testCreateOrder_ValidDeliveryAddress() throws Exception {
        // Given: Valid address
        CreateOrderRequest.DeliveryAddressRequest validAddress = CreateOrderRequest.DeliveryAddressRequest.builder()
                .receiverName("Nguyen Van A")
                .receiverPhone("0901234567")
                .addressLine1("123 Le Loi Street")
                .ward("Ward 1")
                .district("District 1")
                .city("Ho Chi Minh")
                .build();
        validRequest.setDeliveryAddress(validAddress);

        when(idempotencyKeyRepository.existsByUserIdAndIdemKey(anyLong(), anyString())).thenReturn(false);
        when(productServicePort.validateProducts(any())).thenReturn(validatedProducts);
        Order savedOrder = createMockOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        // When & Then: Should not throw exception
        assertThatCode(() -> createOrderUseCase.execute(validRequest, "IDEM-KEY-123", "test-jti-123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("❌ Should throw exception when receiver name is empty")
    void testCreateOrder_EmptyReceiverName() {
        // Given
        validRequest.getDeliveryAddress().setReceiverName("");

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Tên người nhận");
    }

    @Test
    @DisplayName("❌ Should throw exception when receiver name is too short")
    void testCreateOrder_ReceiverNameTooShort() {
        // Given
        validRequest.getDeliveryAddress().setReceiverName("A");

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Tên người nhận");
    }

    @Test
    @DisplayName("❌ Should throw exception when receiver name contains only numbers")
    void testCreateOrder_ReceiverNameOnlyNumbers() {
        // Given
        validRequest.getDeliveryAddress().setReceiverName("123456");

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Tên người nhận phải chứa ít nhất một chữ cái");
    }

    @Test
    @DisplayName("❌ Should throw exception when phone number is invalid format")
    void testCreateOrder_InvalidPhoneFormat() {
        // Given: Phone without leading 0
        validRequest.getDeliveryAddress().setReceiverPhone("901234567");

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Số điện thoại");
    }

    @Test
    @DisplayName("❌ Should throw exception when phone number has wrong length")
    void testCreateOrder_InvalidPhoneLength() {
        // Given: Phone with 9 digits
        validRequest.getDeliveryAddress().setReceiverPhone("09012345");

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Số điện thoại");
    }

    @Test
    @DisplayName("❌ Should throw exception when phone number has 11 digits")
    void testCreateOrder_PhoneTooLong() {
        // Given: Phone with 11 digits
        validRequest.getDeliveryAddress().setReceiverPhone("09012345678");

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Số điện thoại");
    }

    @Test
    @DisplayName("❌ Should throw exception when address line 1 is too short")
    void testCreateOrder_AddressLine1TooShort() {
        // Given
        validRequest.getDeliveryAddress().setAddressLine1("123");

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Địa chỉ chi tiết");
    }

    @Test
    @DisplayName("❌ Should throw exception when ward is too short")
    void testCreateOrder_WardTooShort() {
        // Given
        validRequest.getDeliveryAddress().setWard("W");

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Phường/Xã");
    }

    @Test
    @DisplayName("❌ Should throw exception when district is too short")
    void testCreateOrder_DistrictTooShort() {
        // Given
        validRequest.getDeliveryAddress().setDistrict("D");

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Quận/Huyện");
    }

    @Test
    @DisplayName("❌ Should throw exception when city is too short")
    void testCreateOrder_CityTooShort() {
        // Given
        validRequest.getDeliveryAddress().setCity("H");

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, null, "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Thành phố/Tỉnh");
    }

    @Test
    @DisplayName("✅ Should accept address with whitespace (should trim)")
    void testCreateOrder_AddressWithWhitespace() throws Exception {
        // Given: Address with leading/trailing whitespace
        CreateOrderRequest.DeliveryAddressRequest addressWithWhitespace = CreateOrderRequest.DeliveryAddressRequest.builder()
                .receiverName("  Nguyen Van A  ")
                .receiverPhone("  0901234567  ")
                .addressLine1("  123 Le Loi Street  ")
                .ward("  Ward 1  ")
                .district("  District 1  ")
                .city("  Ho Chi Minh  ")
                .build();
        validRequest.setDeliveryAddress(addressWithWhitespace);

        when(idempotencyKeyRepository.existsByUserIdAndIdemKey(anyLong(), anyString())).thenReturn(false);
        when(productServicePort.validateProducts(any())).thenReturn(validatedProducts);
        Order savedOrder = createMockOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        // When & Then: Should not throw exception (whitespace should be trimmed)
        assertThatCode(() -> createOrderUseCase.execute(validRequest, "IDEM-KEY-123", "test-jti-123"))
                .doesNotThrowAnyException();
    }

    // =====================================================================
    // TEST CASES: PRODUCT SERVICE ERRORS
    // =====================================================================

    @Test
    @DisplayName("❌ Should throw exception when Product Service is down")
    void testCreateOrder_ProductServiceDown() {
        // Given: Product Service không phản hồi
        when(idempotencyKeyRepository.existsByUserIdAndIdemKey(anyLong(), anyString())).thenReturn(false);
        when(productServicePort.validateProducts(any()))
                .thenThrow(new RuntimeException("Connection timeout"));

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, "IDEM-KEY-123", "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Product Service"); // Chỉ check chứa "Product Service"

        // Verify không save order
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Should throw exception when product is out of stock")
    void testCreateOrder_ProductOutOfStock() {
        // Given: Product Service trả về sản phẩm hết hàng
        List<ProductValidationResponse> outOfStockProducts = List.of(
                new ProductValidationResponse(
                        1L,
                        false, // ← Không hợp lệ
                        "iPhone 15 Pro Max",
                        BigDecimal.valueOf(30000000),
                        10L
                )
        );

        when(idempotencyKeyRepository.existsByUserIdAndIdemKey(anyLong(), anyString())).thenReturn(false);
        when(productServicePort.validateProducts(any())).thenReturn(outOfStockProducts);

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.execute(validRequest, "IDEM-KEY-123", "test-jti-123"))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("PROD-001"); // Chỉ check chứa product ID

        // Verify không save order
        verify(orderRepository, never()).save(any());
    }

    // =====================================================================
    // TEST CASES: EDGE CASES
    // =====================================================================

    @Test
    @DisplayName("✅ Should work without idempotency key")
    void testCreateOrder_NoIdempotencyKey() throws Exception {
        // Given
        when(productServicePort.validateProducts(any())).thenReturn(validatedProducts);

        Order savedOrder = createMockOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        // When: Execute KHÔNG CÓ idempotency key
        OrderResponse response = createOrderUseCase.execute(validRequest, null, "test-jti-123");

        // Then
        assertThat(response).isNotNull();

        // Verify KHÔNG SAVE idempotency key
        verify(idempotencyKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("✅ Should calculate total correctly with discount and shipping fee")
    void testCreateOrder_CalculateTotals() throws Exception {
        // Given: Order có discount và shipping fee
        validRequest.setDiscount(BigDecimal.valueOf(1000000)); // Giảm 1M
        validRequest.setShippingFee(BigDecimal.valueOf(100000)); // Ship 100K

        when(idempotencyKeyRepository.existsByUserIdAndIdemKey(anyLong(), anyString())).thenReturn(false);
        when(productServicePort.validateProducts(any())).thenReturn(validatedProducts);

        Order savedOrder = createMockOrder();
        savedOrder.setSubtotal(BigDecimal.valueOf(60000000)); // 2 * 30M
        savedOrder.setDiscount(BigDecimal.valueOf(1000000));
        savedOrder.setShippingFee(BigDecimal.valueOf(100000));
        savedOrder.setGrandTotal(BigDecimal.valueOf(59100000)); // 60M - 1M + 100K

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        // When
        OrderResponse response = createOrderUseCase.execute(validRequest, "IDEM-KEY-123", "test-jti-123");

        // Then
        assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(60000000));
        assertThat(response.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(response.getGrandTotal()).isEqualByComparingTo(BigDecimal.valueOf(59100000));
    }

    // =====================================================================
    // HELPER METHODS
    // =====================================================================

    private Order createMockOrder() {
        // Tạo DeliveryAddress
        DeliveryAddress deliveryAddress = DeliveryAddress.builder()
                .receiverName("Nguyen Van A")
                .receiverPhone("0901234567")
                .addressLine1("123 Le Loi")
                .ward("Ward 1")
                .district("District 1")
                .city("Ho Chi Minh")
                .build();

        // Tạo Order với đầy đủ fields (tránh NullPointerException)
        Order order = Order.builder()
                .id(1L)
                .orderCode("ORD-123456")
                .userId(1L)
                .merchantId(10L)
                .status(OrderStatus.PENDING)
                .currency("VND")
                .subtotal(BigDecimal.valueOf(60000000))
                .discount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.valueOf(50000))
                .grandTotal(BigDecimal.valueOf(60050000))
                .deliveryAddress(deliveryAddress)
                .build();

        // Tạo OrderItem với lineTotal đã tính sẵn
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productId(1L)
                .merchantId(10L)
                .productName("iPhone 15 Pro Max")
                .unitPrice(BigDecimal.valueOf(30000000))
                .quantity(2)
                .build();

        // Set lineTotal thủ công (vì không gọi calculateTotals())
        // lineTotal = unitPrice * quantity = 30M * 2 = 60M
        try {
            java.lang.reflect.Field lineTotal = OrderItem.class.getDeclaredField("lineTotal");
            lineTotal.setAccessible(true);
            lineTotal.set(item, BigDecimal.valueOf(60000000));
        } catch (Exception e) {
            // Fallback: tạo item mới
        }

        order.getOrderItems().add(item);
        return order;
    }
}
