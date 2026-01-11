package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.*;
import com.example.order_service.application.service.AdministrativeAddressNormalizer;
import com.example.order_service.domain.repository.ProductServicePort;
import com.example.order_service.domain.repository.UserServicePort;
import com.example.order_service.domain.repository.UserAddressRepository;
import com.example.order_service.infrastructure.event.OrderCreatedEventPayload;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.entities.Order;
import com.example.order_service.domain.entities.OrderItem;
import com.example.order_service.domain.entities.OutboxEvent;
import com.example.order_service.domain.entities.UserAddress;
import com.example.order_service.domain.valueobjects.OrderStatus;
import com.example.order_service.domain.valueobjects.OrderCode;
import com.example.order_service.domain.valueobjects.Money;
import com.example.order_service.domain.valueobjects.DeliveryAddress;
import com.example.order_service.domain.valueobjects.EventStatus;
import com.example.order_service.domain.entities.IdempotencyKey;
import com.example.order_service.domain.repository.IdempotencyKeyRepository;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

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
@Transactional // Đảm bảo tất cả chạy trong 1 transaction
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository; // khi tạo order sinh key để tránh trùng 2 order
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ProductServicePort productServicePort;// Port để gọi sang Product Service
    private final UserServicePort userServicePort;// Port để gọi sang User Service
    private final AdministrativeAddressNormalizer administrativeAddressNormalizer;
    private final UserAddressRepository userAddressRepository;
    @Value("${app.user.validation.enabled:true}")
    private boolean userValidationEnabled;

    @Value("${app.merchant.validation.enabled:true}")
    private boolean merchantValidationEnabled;

    /**
     * MAIN METHOD - TẠO ORDER
     * Flow: Validate -> Check duplicate -> Call Product Service -> Create Order ->
     * Save -> Event
     */

    @Transactional
    public OrderResponse execute(CreateOrderRequest request, String idempotencyKey, String jti) {
        log.info("=== CreateOrderUseCase.execute() called ===");
        log.info("Creating order for user: {}", request.getUserId());
        log.info("🔑 Idempotency-Key received in UseCase: '{}'", idempotencyKey);
        log.info("🔐 JWT jti (audit): {}", jti);
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            log.info("✅ Idempotency-Key provided: '{}' - System will check for duplicate requests", idempotencyKey);
        } else {
            log.info("ℹ️ No Idempotency-Key provided - New order will be created");
        }

        // Validate request
        validateRequest(request);

        // ===== BƯỚC 2: CHECK IDEMPOTENCY (CHỐNG DUPLICATE REQUEST) =====
        if (isDuplicateRequest(request.getUserId(), idempotencyKey)) {
            log.warn("⚠️ Duplicate request detected! Idempotency-Key: '{}' for userId: {}", idempotencyKey,
                    request.getUserId());
            log.warn(
                    "⚠️ Returning existing order instead of creating new one. To create a new order, use a different Idempotency-Key or don't send the header.");
            log.warn(
                    "⚠️ If you changed the key in Postman but still see this error, the header may not be forwarded correctly by the gateway.");
            return getExistingOrderResponse(request.getUserId(), idempotencyKey);
        }
        // ===== BƯỚC 3: GỌI PRODUCT SERVICE ĐỂ LẤY THÔNG TIN SẢN PHẨM =====
        // Đây là bước QUAN TRỌNG NHẤT - lấy giá và tên thực tế từ Product Service
        List<ProductValidationResponse> validatedProducts = callProductServiceForValidation(request);

        // ===== BƯỚC 4: RESOLVE DELIVERY ADDRESS =====
        DeliveryAddress resolvedDeliveryAddress = resolveDeliveryAddress(request);

        // ===== BƯỚC 5: TẠO ORDER TỪ DATA ĐÃ VALIDATE =====
        Order order = buildOrderFromValidatedData(request, validatedProducts, resolvedDeliveryAddress);
        order = orderRepository.save(order);
        log.info("Order saved with code: {}", order.getOrderCode());

        // ===== BƯỚC 5: LƯU IDEMPOTENCY KEY (NẾU CÓ) =====
        saveIdempotencyKeyIfProvided(request, idempotencyKey, order);

        // ===== BƯỚC 6: TẠO OUTBOX EVENT CHO RABBITMQ =====
        createOutboxEventForRabbitMQ(order);

        log.info("🎉 Order created successfully: {} for user: {}", order.getOrderCode().getValue(),
                request.getUserId());
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            log.info("✅ Idempotency-Key saved: {} - This key can be reused to retrieve this order", idempotencyKey);
        }
        return OrderResponse.fromEntity(order);

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

        // Validate delivery address format and business rules
        if (request.getDeliveryAddressId() == null) {
            validateDeliveryAddress(request.getDeliveryAddress());
        } else {
            validateReceiverInfo(request.getDeliveryAddress());
        }

        // ===== PHASE 2: USER SERVICE VALIDATION (sau khi validate dữ liệu đầu vào)
        // =====
        if (userValidationEnabled) {
            validateUser(request.getUserId());
        }
    }

    /**
     * Validate user exists and is active
     * Calls User Service to check if user exists and is active
     */
    private void validateUser(Long userId) {
        log.debug("Validating user: {}", userId);

        try {
            UserValidationResponse user = userServicePort.validateUser(userId);

            if (!user.exists()) {
                log.error("User {} does not exist", userId);
                throw new OrderValidationException("User không tồn tại: " + userId);
            }

            if (!user.active()) {
                log.error("User {} is not active", userId);
                throw new OrderValidationException("User không active: " + userId);
            }

            log.debug("✓ User {} validated successfully (exists: {}, active: {})",
                    userId, user.exists(), user.active());
        } catch (OrderValidationException e) {
            // Re-throw OrderValidationException
            throw e;
        } catch (Exception e) {
            log.error("User Service call failed for userId: {}", userId, e);
            throw new OrderValidationException("User Service không phản hồi: " + e.getMessage());
        }
    }

    /**
     * Validate delivery address business rules
     * Note: Basic validation (not null, not blank, size, pattern) is handled by
     * Bean Validation annotations.
     * This method only validates business rules that cannot be expressed via
     * annotations:
     * - Receiver name must contain at least one letter (business rule)
     * - Lat/Lng validation (both must be provided together, valid ranges)
     */
    private void validateDeliveryAddress(CreateOrderRequest.DeliveryAddressRequest address) {
        if (address == null) {
            // Thêm từ khóa "address" để compatible với test messageContains("address")
            throw new OrderValidationException("Dia chi giao hang (delivery address) la bat buoc");
        }

        validateReceiverInfo(address);

        // ===== BASIC VALIDATION: Receiver name (empty / too short) =====
        String addressLine1 = address.getAddressLine1() != null ? address.getAddressLine1().trim() : "";
        if (addressLine1.isEmpty() || addressLine1.length() < 5) {
            throw new OrderValidationException("Địa chỉ chi tiết quá ngắn");
        }

        // ===== BASIC VALIDATION: Ward =====
        String ward = address.getWard() != null ? address.getWard().trim() : "";
        if (ward.isEmpty() || ward.length() < 2) {
            throw new OrderValidationException("Phường/Xã quá ngắn");
        }

        // ===== BASIC VALIDATION: District =====
        String district = address.getDistrict() != null ? address.getDistrict().trim() : "";
        if (district.isEmpty() || district.length() < 2) {
            throw new OrderValidationException("Quận/Huyện quá ngắn");
        }

        // ===== BASIC VALIDATION: City =====
        String city = address.getCity() != null ? address.getCity().trim() : "";
        if (city.isEmpty() || city.length() < 2) {
            throw new OrderValidationException("Thành phố/Tỉnh quá ngắn");
        }

        // ===== BUSINESS RULE: Validate Lat/Lng (Optional) =====
        if (address.getLat() != null || address.getLng() != null) {
            // If either lat or lng is provided, both must be provided
            if (address.getLat() == null || address.getLng() == null) {
                throw new OrderValidationException("Tọa độ không hợp lệ: phải cung cấp cả lat và lng");
            }

            // Validate lat range: -90 to 90
            BigDecimal lat = address.getLat();
            if (lat.compareTo(new BigDecimal("-90")) < 0 || lat.compareTo(new BigDecimal("90")) > 0) {
                throw new OrderValidationException("Tọa độ không hợp lệ: lat phải trong khoảng -90 đến 90");
            }

            // Validate lng range: -180 to 180
            BigDecimal lng = address.getLng();
            if (lng.compareTo(new BigDecimal("-180")) < 0 || lng.compareTo(new BigDecimal("180")) > 0) {
                throw new OrderValidationException("Tọa độ không hợp lệ: lng phải trong khoảng -180 đến 180");
            }
        }

        log.debug("✓ Delivery address business rules validated successfully");
    }

    private void validateReceiverInfo(CreateOrderRequest.DeliveryAddressRequest address) {
        if (address == null) {
            throw new OrderValidationException("Thông tin người nhận là bắt buộc");
        }
        String receiverName = address.getReceiverName() != null ? address.getReceiverName().trim() : "";
        if (receiverName.isEmpty() || receiverName.length() < 2) {
            throw new OrderValidationException("Tên người nhận không được để trống và phải có ít nhất 2 ký tự");
        }
        if (!receiverName.matches(".*[\\p{L}].*")) {
            throw new OrderValidationException("Tên người nhận phải chứa ít nhất một chữ cái");
        }

        String phone = address.getReceiverPhone() != null ? address.getReceiverPhone().trim() : "";
        if (phone.isEmpty() || !phone.matches("^0\\d{9}$")) {
            throw new OrderValidationException("Số điện thoại không hợp lệ");
        }
    }

    /**
     * Kiểm tra xem request này đã được xử lý chưa (dựa vào idempotency key)
     */
    private boolean isDuplicateRequest(Long userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            log.debug("No idempotency key provided, skipping duplicate check");
            return false; // Không có key thì không check
        }
        log.debug("Checking for duplicate request: userId={}, idempotencyKey='{}'", userId, idempotencyKey);
        boolean exists = idempotencyKeyRepository.existsByUserIdAndIdemKey(userId, idempotencyKey);
        log.debug("Duplicate check result: {}", exists);
        return exists;
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

        return OrderResponse.fromEntity(existingOrder);
    }

    /**
     * GỌI PRODUCT SERVICE ĐỂ VALIDATE VÀ LẤY THÔNG TIN SẢN PHẨM
     * Đây là bước mày đang muốn làm!
     */
    private List<ProductValidationResponse> callProductServiceForValidation(CreateOrderRequest request) {
        log.debug("Calling Product Service to validate {} items...", request.getOrderItems().size());

        // Logging để debug
        request.getOrderItems().forEach(item -> {
            log.debug("Item: productId={}, quantity={}",
                    item.getProductId(),
                    item.getQuantity());
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

            // CRITICAL: Log response
            log.info("Received {} responses from Product Service",
                    validatedProducts != null ? validatedProducts.size() : 0);

            if (validatedProducts != null) {
                validatedProducts.forEach(vp -> log.info("Response: productId={}, success={}, name={}, price={}",
                        vp.productId(), vp.success(), vp.productName(), vp.unitPrice()));
            }
        } catch (OrderValidationException e) {
            // Re-throw OrderValidationException từ Circuit Breaker fallback
            log.error("Product Service validation failed: {}", e.getMessage());
            throw e;
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
            List<ProductValidationResponse> validatedProducts,
            DeliveryAddress resolvedDeliveryAddress) {
        // Tạo Map để tra cứu nhanh thông tin sản phẩm đã validate
        Map<Long, ProductValidationResponse> productMap = validatedProducts.stream()
                .collect(Collectors.toMap(ProductValidationResponse::productId, p -> p));

        // Validate all products belong to the same merchant
        Long merchantId = validateSingleMerchant(validatedProducts);
        if (merchantValidationEnabled) {
            ensureMerchantIsActive(merchantId);
        }

        // Tạo Order với value objects
        String currency = "VND";
        Order order = Order.builder()
                .orderCode(new OrderCode(generateOrderCode()))
                .userId(request.getUserId())
                .merchantId(merchantId)
                .status(OrderStatus.PENDING)
                .discount(new Money(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO, currency))
                .shippingFee(new Money(request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO,
                        currency))
                .note(request.getNote())
                .deliveryAddress(resolvedDeliveryAddress)
                .createdAt(LocalDateTime.now())
                .build();

        // Thêm OrderItems (LẤY TẤT CẢ THÔNG TIN TỪ PRODUCT SERVICE)
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getOrderItems()) {
            ProductValidationResponse validatedProduct = productMap.get(itemRequest.getProductId());

            if (validatedProduct == null) {
                throw new OrderValidationException(
                        "Sản phẩm " + itemRequest.getProductId() + " không tìm thấy trong Product Service");
            }

            if (!validatedProduct.success()) {
                throw new OrderValidationException(
                        "Sản phẩm " + itemRequest.getProductId() + " không hợp lệ hoặc hết hàng");
            }

            // Lấy tất cả thông tin từ Product Service và tạo với Money value object
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .merchantId(validatedProduct.merchantId()) // Set merchantId from product
                    .productName(validatedProduct.productName()) // Lấy từ Product Service
                    .unitPrice(new Money(validatedProduct.unitPrice(), currency)) // Money value object
                    .quantity(itemRequest.getQuantity())
                    .build();

            // Tính lineTotal sử dụng domain entity method
            orderItem.calculateLineTotal();

            log.debug(
                    "Created order item: productId={}, merchantId={}, productName={}, unitPrice={}, quantity={}, lineTotal={}",
                    orderItem.getProductId(),
                    orderItem.getMerchantId(),
                    orderItem.getProductName(),
                    orderItem.getUnitPrice(),
                    orderItem.getQuantity(),
                    orderItem.getLineTotal());

            order.addOrderItem(orderItem);
        }

        // Tính tổng tiền
        order.calculateTotals();

        return order;
    }

    /**
     * Validate that all products belong to the same merchant
     * 
     * @param validatedProducts List of validated products
     * @return The merchantId that all products belong to
     * @throws OrderValidationException if products belong to different merchants
     */
    private Long validateSingleMerchant(List<ProductValidationResponse> validatedProducts) {
        if (validatedProducts == null || validatedProducts.isEmpty()) {
            throw new OrderValidationException("Order must contain at least one product");
        }

        Long firstMerchantId = validatedProducts.get(0).merchantId();
        if (firstMerchantId == null) {
            throw new OrderValidationException("Product merchantId cannot be null");
        }

        // Check all products have the same merchantId
        for (ProductValidationResponse product : validatedProducts) {
            if (product.merchantId() == null || !product.merchantId().equals(firstMerchantId)) {
                throw new OrderValidationException(
                        "All products in an order must belong to the same merchant. " +
                                "Found products from different merchants.");
            }
        }

        log.debug("All products validated to belong to merchant: {}", firstMerchantId);
        return firstMerchantId;
    }

    private void ensureMerchantIsActive(Long merchantId) {
        try {
            UserValidationResponse merchant = userServicePort.validateUser(merchantId);

            if (!merchant.exists()) {
                throw new OrderValidationException("Merchant không tồn tại: " + merchantId);
            }

            if (!merchant.active()) {
                throw new OrderValidationException("Merchant không đang hoạt động: " + merchantId);
            }
        } catch (OrderValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to validate merchant {} status", merchantId, e);
            throw new OrderValidationException("Không thể xác thực merchant: " + merchantId);
        }
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
                .merchantId(order.getMerchantId()) // Include merchantId in event payload
                .grandTotal(order.getGrandTotal() != null ? order.getGrandTotal().getAmount() : null)
                .currency(order.getGrandTotal() != null ? order.getGrandTotal().getCurrency() : "VND")
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
        // Chuẩn hóa địa chỉ hành chính (nếu có thể) bằng AddressKit
        AdministrativeAddressNormalizer.NormalizedAddress normalized = null;
        try {
            normalized = administrativeAddressNormalizer
                    .normalizeForHcm(request.getCity(), request.getDistrict(), request.getWard())
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Failed to normalize administrative address. Proceeding without normalized fields. Reason: {}",
                    ex.getMessage());
        }

        String provinceCode = request.getProvinceCode();
        String provinceName = request.getProvinceName();
        String communeCode = request.getCommuneCode();
        String communeName = request.getCommuneName();
        String normalizedDistrictName = request.getNormalizedDistrictName();

        if (normalized != null) {
            // Ưu tiên sử dụng dữ liệu chuẩn hóa nếu có
            if (provinceCode == null)
                provinceCode = normalized.getProvinceCode();
            if (provinceName == null)
                provinceName = normalized.getProvinceName();
            if (communeCode == null)
                communeCode = normalized.getCommuneCode();
            if (communeName == null)
                communeName = normalized.getCommuneName();
            if (normalizedDistrictName == null)
                normalizedDistrictName = normalized.getNormalizedDistrictName();
        }

        return DeliveryAddress.builder()
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .addressLine1(request.getAddressLine1())
                .ward(request.getWard())
                .district(request.getDistrict())
                .city(request.getCity())
                .provinceCode(provinceCode)
                .provinceName(provinceName)
                .communeCode(communeCode)
                .communeName(communeName)
                .normalizedDistrictName(normalizedDistrictName)
                .lat(request.getLat())
                .lng(request.getLng())
                .build();
    }

    private DeliveryAddress resolveDeliveryAddress(CreateOrderRequest request) {
        if (request.getDeliveryAddressId() == null) {
            return mapToDeliveryAddress(request.getDeliveryAddress());
        }

        var receiverInfo = request.getDeliveryAddress();
        validateReceiverInfo(receiverInfo);

        var userAddress = userAddressRepository.findById(request.getDeliveryAddressId())
                .orElseThrow(() -> new OrderValidationException("Địa chỉ đã chọn không tồn tại"));
        if (!userAddress.getUserId().equals(request.getUserId())) {
            throw new OrderValidationException("Địa chỉ không thuộc sở hữu của người dùng");
        }

        return DeliveryAddress.builder()
                .receiverName(receiverInfo.getReceiverName())
                .receiverPhone(receiverInfo.getReceiverPhone())
                .addressLine1(userAddress.getStreet())
                .ward(userAddress.getCommuneName())
                .district(userAddress.getDistrictName() != null ? userAddress.getDistrictName() : "Not Specified")
                .city(userAddress.getProvinceName())
                .provinceCode(userAddress.getProvinceCode())
                .provinceName(userAddress.getProvinceName())
                .communeCode(userAddress.getCommuneCode())
                .communeName(userAddress.getCommuneName())
                .normalizedDistrictName(userAddress.getDistrictName())
                .lat(userAddress.getLat())
                .lng(userAddress.getLng())
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

}
