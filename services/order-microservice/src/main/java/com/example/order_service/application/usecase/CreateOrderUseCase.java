package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.CreateOrderRequest;
import com.example.order_service.application.dto.OrderResponse;
import com.example.order_service.application.dto.ProductValidationRequest;
import com.example.order_service.application.dto.ProductValidationResponse;
import com.example.order_service.domain.repository.ProductServicePort;
import com.example.order_service.infrastructure.event.OrderCreatedEventPayload;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.model.*;
import com.example.order_service.domain.repository.IdempotencyKeyRepository;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional //Đảm bảo tất cả chạy trong 1 transaction
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository; // khi tạo order sinh key để tránh trùng 2 order
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ProductServicePort productServicePort;// Port để gọi sang Product Service

    /**
     * MAIN METHOD - TẠO ORDER
     * Flow: Validate -> Check duplicate -> Call Product Service -> Create Order -> Save -> Event
     */

    @Transactional
    public OrderResponse execute(CreateOrderRequest request, String idempotencyKey) {
        log.info("Creating order for user: {}", request.getUserId());

        // Validate request
        validateRequest(request);

        // ===== BƯỚC 2: CHECK IDEMPOTENCY (CHỐNG DUPLICATE REQUEST) =====
        if (isDuplicateRequest(request.getUserId(), idempotencyKey)) {
            log.warn("Duplicate request detected with key: {}", idempotencyKey);
            return getExistingOrderResponse(request.getUserId(), idempotencyKey);
        }
        // ===== BƯỚC 3: GỌI PRODUCT SERVICE ĐỂ LẤY THÔNG TIN SẢN PHẨM =====
        // Đây là bước QUAN TRỌNG NHẤT - lấy giá và tên thực tế từ Product Service
        List<ProductValidationResponse> validatedProducts = callProductServiceForValidation(request);

        // ===== BƯỚC 4: TẠO ORDER TỪ DATA ĐÃ VALIDATE =====
        Order order = buildOrderFromValidatedData(request, validatedProducts);
        order = orderRepository.save(order);
        log.info("Order saved with code: {}", order.getOrderCode());

        // ===== BƯỚC 5: LƯU IDEMPOTENCY KEY (NẾU CÓ) =====
        saveIdempotencyKeyIfProvided(request, idempotencyKey, order);

        // ===== BƯỚC 6: TẠO OUTBOX EVENT CHO RABBITMQ =====
        createOutboxEventForRabbitMQ(order);

        log.info("🎉 Order created successfully: {}", order.getOrderCode());
        return mapToResponse(order);

    }

    private void validateRequest(CreateOrderRequest request) {
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new OrderValidationException("User ID khong hop le: " + request.getUserId());
        }

        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new OrderValidationException("Order phai co it nhat 1 san pham");
        }

        if (request.getDeliveryAddress() == null) {
            throw new OrderValidationException("Dia chi giao hang la bat buoc");
        }
    }

    /**
     * Kiểm tra xem request này đã được xử lý chưa (dựa vào idempotency key)
     */
    private boolean isDuplicateRequest(Long userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return false; // Không có key thì không check
        }
        return idempotencyKeyRepository.existsByUserIdAndIdemKey(userId, idempotencyKey);
    }

    /**
     * Lấy order đã tồn tại (trường hợp duplicate request)
     */
    private OrderResponse getExistingOrderResponse(Long userId, String idempotencyKey) {
        IdempotencyKey existingKey = idempotencyKeyRepository
                .findByUserIdAndIdemKey(userId, idempotencyKey)
                .orElseThrow(() -> new OrderValidationException("Idempotency key không tìm thấy"));

        Order existingOrder = orderRepository.findById(existingKey.getOrderId())
                .orElseThrow(() -> new OrderValidationException("Order không tìm thấy"));

        return mapToResponse(existingOrder);
    }
    /**
     * GỌI PRODUCT SERVICE ĐỂ VALIDATE VÀ LẤY THÔNG TIN SẢN PHẨM
     * Đây là bước mày đang muốn làm!
     */
    private List<ProductValidationResponse> callProductServiceForValidation(CreateOrderRequest request) {
        log.debug("Calling Product Service to validate {} items...", request.getOrderItems().size());

        //  THÊM LOGGING ĐỂ DEBUG
        request.getOrderItems().forEach(item -> {
            log.debug("Item: productId={}, productName={}, quantity={}, unitPrice={}",
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice());
        });

        // Chuẩn bị danh sách cần validate (productId + quantity)
        List<ProductValidationRequest> validationRequests = request.getOrderItems().stream()
                .map(item -> new ProductValidationRequest(item.getProductId(), item.getQuantity()))
                .toList();


        // Gọi Product Service qua Port (FeignClient hoặc RestTemplate)
        List<ProductValidationResponse> validatedProducts;
        try {
            log.info("Sending validation request to Product Service...");
            validatedProducts = productServicePort.validateProducts(validationRequests);

            //  CRITICAL: Log response
            log.info("Received {} responses from Product Service",
                    validatedProducts != null ? validatedProducts.size() : 0);

            if (validatedProducts != null) {
                validatedProducts.forEach(vp ->
                        log.info("Response: productId={}, success={}, name={}, price={}",
                                vp.productId(), vp.success(), vp.productName(), vp.unitPrice())
                );
            }
        } catch (Exception e) {
            log.error("Product Service call failed", e);
            throw new OrderValidationException("Product Service không phản hồi: " + e.getMessage());
        }

        // Kiểm tra kết quả: Tất cả sản phẩm phải hợp lệ (còn hàng, tồn tại)
        List<String> invalidProducts = validatedProducts.stream()
                .filter(p -> !p.success())
                .map(p -> "Product " + p.productId() + " không hợp lệ/hết hàng")
                .toList();

        if (!invalidProducts.isEmpty()) {
            String errorMsg = String.join(", ", invalidProducts);
            log.error("Validation errors: {}", errorMsg);
            throw new OrderValidationException(errorMsg);
        }

        log.debug("✓ All products validated successfully");
        return validatedProducts;
    }
    /**
     * XÂY DỰNG ORDER TỪ DATA ĐÃ VALIDATE
     * Lưu ý: Dùng giá và tên từ Product Service, KHÔNG DÙNG GIÁ TỪ REQUEST
     */
    private Order buildOrderFromValidatedData(
            CreateOrderRequest request,
            List<ProductValidationResponse> validatedProducts
    ) {
        // Tạo Map để tra cứu nhanh thông tin sản phẩm đã validate
        Map<Long, ProductValidationResponse> productMap = validatedProducts.stream()
                .collect(Collectors.toMap(ProductValidationResponse::productId, p -> p));

        // Tạo Order
        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .currency("VND")
                .discount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO)
                .shippingFee(request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO)
                .note(request.getNote())
                .deliveryAddress(mapToDeliveryAddress(request.getDeliveryAddress()))
                .createdAt(LocalDateTime.now())
                .build();

        // Thêm OrderItems (DÙNG GIÁ VÀ TÊN TỪ PRODUCT SERVICE)
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getOrderItems()) {
            ProductValidationResponse validatedProduct = productMap.get(itemRequest.getProductId());

            if (validatedProduct == null) {
                throw new OrderValidationException("Sản phẩm " + itemRequest.getProductId() + " không tìm thấy");
            }

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName(validatedProduct.productName()) // Lấy từ Product Service
                    .unitPrice(validatedProduct.unitPrice())     // Lấy từ Product Service (QUAN TRỌNG!)
                    .quantity(itemRequest.getQuantity())
                    .build();

            // ✅ THÊM: Tính lineTotal ngay sau khi build
            orderItem.setLineTotal(
                    validatedProduct.unitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
            );

            order.addOrderItem(orderItem);
        }

        // Tính tổng tiền
        order.calculateTotals();

        return order;
    }
    /**
     * Lưu idempotency key để chống duplicate request
     */
    private void saveIdempotencyKeyIfProvided(CreateOrderRequest request, String idempotencyKey, Order order) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return; // Không có key thì thôi
        }

        String requestHash = calculateRequestHash(request);
        IdempotencyKey idemKey = IdempotencyKey.builder()
                .userId(request.getUserId())
                .idemKey(idempotencyKey)
                .requestHash(requestHash)
                .orderId(order.getId())
                .createdAt(LocalDateTime.now())
                .build();

        idempotencyKeyRepository.save(idemKey);
        log.debug("✓ Idempotency key saved");
    }

    /**
     * Tạo Outbox Event để RabbitMQ đọc và publish
     */
    private void createOutboxEventForRabbitMQ(Order order) {
        OrderCreatedEventPayload payload = OrderCreatedEventPayload.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .grandTotal(order.getGrandTotal())
                .currency(order.getCurrency())
                .build();

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .type("OrderCreated")
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(event);
            log.debug("✓ Outbox event created");

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload for orderId: {}", order.getId(), e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
// =====================================================================
    // UTILITY METHODS
    // =====================================================================

    private DeliveryAddress mapToDeliveryAddress(CreateOrderRequest.DeliveryAddressRequest request) {
        return DeliveryAddress.builder()
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .addressLine1(request.getAddressLine1())
                .ward(request.getWard())
                .district(request.getDistrict())
                .city(request.getCity())
                .build();
    }


    private String generateOrderCode() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String calculateRequestHash(CreateOrderRequest request) {
        try {
            String data = request.getUserId() + request.getOrderItems().toString() +
                    request.getDeliveryAddress().toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculating request hash", e);
        }
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .currency(order.getCurrency())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getGrandTotal())
                .note(order.getNote())
                .deliveryAddress(mapToDeliveryAddressResponse(order.getDeliveryAddress()))
                .orderItems(order.getOrderItems().stream()
                        .map(this::mapToOrderItemResponse)
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderResponse.DeliveryAddressResponse mapToDeliveryAddressResponse(DeliveryAddress deliveryAddress) {
        return OrderResponse.DeliveryAddressResponse.builder()
                .receiverName(deliveryAddress.getReceiverName())
                .receiverPhone(deliveryAddress.getReceiverPhone())
                .addressLine1(deliveryAddress.getAddressLine1())
                .ward(deliveryAddress.getWard())
                .district(deliveryAddress.getDistrict())
                .city(deliveryAddress.getCity())
                .fullAddress(deliveryAddress.getFullAddress())
                .build();
    }

    private OrderResponse.OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        return OrderResponse.OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .productName(orderItem.getProductName())
                .unitPrice(orderItem.getUnitPrice())
                .quantity(orderItem.getQuantity())
                .lineTotal(orderItem.getLineTotal())
                .build();
    }
}
