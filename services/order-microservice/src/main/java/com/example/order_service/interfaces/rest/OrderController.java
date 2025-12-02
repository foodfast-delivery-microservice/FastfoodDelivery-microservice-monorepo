package com.example.order_service.interfaces.rest;

import com.example.order_service.application.dto.*;
import com.example.order_service.application.usecase.CreateOrderUseCase;
import com.example.order_service.application.usecase.GetMerchantOrdersUseCase;
import com.example.order_service.application.usecase.GetOrderDetailUseCase;
import com.example.order_service.application.usecase.GetOrderListUseCase;
import com.example.order_service.application.usecase.GetUserStatisticsUseCase;
import com.example.order_service.application.usecase.RequestRefundUseCase;
import com.example.order_service.application.usecase.UpdateOrderStatusUseCase;
import com.example.order_service.domain.exception.InvalidJwtTokenException;
import com.example.order_service.domain.exception.MerchantOrderAccessDeniedException;
import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.infrastructure.security.JwtTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderListUseCase getOrderListUseCase;
    private final GetOrderDetailUseCase getOrderDetailUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final GetMerchantOrdersUseCase getMerchantOrdersUseCase;
    private final RequestRefundUseCase requestRefundUseCase;
    private final GetUserStatisticsUseCase getUserStatisticsUseCase;
    private final JwtTokenService jwtTokenService;

    /**
     * Tạo đơn hàng mới
     * POST /api/v1/orders
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // ===== PHASE 1: SECURITY - Extract userId from JWT token =====
        Long userIdFromToken;
        try {
            // Validate token first
            jwtTokenService.validateToken(jwt);
            // Extract userId from token
            userIdFromToken = jwtTokenService.extractUserId(jwt);
            log.info("✓ Successfully extracted userId {} from JWT token", userIdFromToken);
        } catch (InvalidJwtTokenException e) {
            log.error("Failed to extract userId from JWT token: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while processing JWT token: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token không hợp lệ: " + e.getMessage());
        }

        // Always override the userId in the request to prevent spoofing
        request.setUserId(userIdFromToken);
        log.debug("UserId {} set in request (from JWT token)", userIdFromToken);

        // 4. Validate scope/role
        var scopes = jwt.getClaimAsStringList("scope");
        String roleClaim = jwt.getClaimAsString("role");
        boolean hasScope = scopes != null && scopes.contains("order:create");
        boolean hasRolePermission = roleClaim != null &&
                (roleClaim.equalsIgnoreCase("USER") ||
                        roleClaim.equalsIgnoreCase("ADMIN") ||
                        roleClaim.equalsIgnoreCase("MERCHANT") ||
                        roleClaim.equalsIgnoreCase("ORDER_CREATE"));

        if (!hasScope && !hasRolePermission) {
            log.error("Missing required scope 'order:create'. Available scopes: {}, roleClaim: {}", scopes, roleClaim);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Thiếu quyền order:create");
        }

        // 5. Extract jti for audit logging
        String jti = jwt.getId();
        log.info("JWT jti (audit): {}", jti);

        // THÊM LOGGING ĐỂ DEBUG
        log.info("=== CREATE ORDER REQUEST ===");
        log.info("UserId from token: {}", request.getUserId());
        log.info("Discount: {}", request.getDiscount());
        log.info("ShippingFee: {}", request.getShippingFee());
        log.info("Note: {}", request.getNote());
        log.info("Number of items: {}", request.getOrderItems() != null ? request.getOrderItems().size() : 0);

        if (request.getOrderItems() != null) {
            request.getOrderItems().forEach(item -> log.info(
                    "OrderItem: productId={}, quantity={} (productName and unitPrice will be fetched from Product Service)",
                    item.getProductId(),
                    item.getQuantity()));
        } else {
            log.warn("OrderItems is NULL!");
        }

        if (request.getDeliveryAddress() != null) {
            log.info("DeliveryAddress: {}, {}, {}",
                    request.getDeliveryAddress().getReceiverName(),
                    request.getDeliveryAddress().getReceiverPhone(),
                    request.getDeliveryAddress().getCity());
        } else {
            log.warn("DeliveryAddress is NULL!");
        }

        log.info("Idempotency-Key from @RequestHeader: {}", idempotencyKey);

        // Log tất cả headers để debug
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            jakarta.servlet.http.HttpServletRequest httpRequest = attributes.getRequest();
            java.util.Enumeration<String> headerNames = httpRequest.getHeaderNames();
            log.info("=== ALL HEADERS IN ORDER SERVICE REQUEST ===");
            while (headerNames != null && headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = httpRequest.getHeader(headerName);
                // Log tất cả headers liên quan đến idempotency, request, trace, correlation
                if (headerName.toLowerCase().contains("idempotency") ||
                        headerName.toLowerCase().contains("idem") ||
                        headerName.toLowerCase().contains("request") ||
                        headerName.toLowerCase().contains("trace") ||
                        headerName.toLowerCase().contains("correlation") ||
                        headerName.toLowerCase().contains("x-")) {
                    log.info("🔍 Header '{}': {}", headerName, headerValue);
                }
            }
            // Đặc biệt log Idempotency-Key header từ HttpServletRequest
            String idempotencyKeyFromRequest = httpRequest.getHeader("Idempotency-Key");
            log.info("🔍 Idempotency-Key from HttpServletRequest: {}", idempotencyKeyFromRequest);
            if (idempotencyKeyFromRequest != null && !idempotencyKeyFromRequest.equals(idempotencyKey)) {
                log.error("❌ MISMATCH! @RequestHeader value: '{}' != HttpServletRequest value: '{}'",
                        idempotencyKey, idempotencyKeyFromRequest);
            }
        }
        log.info("=== END REQUEST ===");
        log.info("Creating order for user: {} (jti: {})", request.getUserId(), jti);

        // Execute use case to create order
        OrderResponse response = createOrderUseCase.execute(request, idempotencyKey, jti);

        // Verify userId is correctly set in response
        if (response.getUserId() == null || !response.getUserId().equals(userIdFromToken)) {
            log.warn("⚠️ UserId mismatch: expected {}, got {}", userIdFromToken, response.getUserId());
            // Override to ensure consistency
            response.setUserId(userIdFromToken);
        }

        log.info("✓ Order created successfully with userId: {}", response.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lấy danh sách đơn hàng
     * GET /api/v1/orders
     */
    @GetMapping
    public ResponseEntity<PageResponse<OrderListResponse>> getOrderList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info(
                "Getting order list with filters - userId: {}, status: {}, orderCode: {}, fromDate: {}, toDate: {}, page: {}, size: {}",
                userId, status, orderCode, fromDate, toDate, page, size);

        OrderListRequest request = OrderListRequest.builder()
                .userId(userId)
                .merchantId(merchantId)
                .status(status)
                .orderCode(orderCode)
                .fromDate(fromDate)
                .toDate(toDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<OrderListResponse> response = getOrderListUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy chi tiết đơn hàng
     * GET /api/v1/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId) {
        log.info("Getting order detail for orderId: {}", orderId);

        try {
            OrderDetailResponse response = getOrderDetailUseCase.execute(orderId);

            if (jwt != null) {
                String role = jwt.getClaimAsString("role");
                Long requesterId = jwtTokenService.extractUserId(jwt);

                if ("MERCHANT".equalsIgnoreCase(role)) {
                    if (requesterId == null || !requesterId.equals(response.getMerchantId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Merchant cannot access orders belonging to other merchants");
                    }
                } else if ("USER".equalsIgnoreCase(role)) {
                    if (requesterId == null || !requesterId.equals(response.getUserId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "You can only view your own orders");
                    }
                }
            }

            return ResponseEntity.ok(response);
        } catch (OrderNotFoundException e) {
            log.warn("Order not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cập nhật trạng thái đơn hàng
     * PUT /api/v1/orders/{orderId}/status
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDetailResponse> updateOrderStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {

        log.info("Updating order status for orderId: {}, request: {}", orderId, request);

        if (isMerchant(jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Merchant must use /api/v1/orders/merchants/me/{id}/status to update orders they own");
        }

        try {
            OrderDetailResponse response = updateOrderStatusUseCase.execute(orderId, request);
            return ResponseEntity.ok(response);
        } catch (OrderNotFoundException e) {
            log.warn("Order not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (OrderValidationException e) {
            log.warn("Order validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tìm kiếm đơn hàng
     * GET /api/v1/orders/search
     */
    @GetMapping("/search")
    public ResponseEntity<PageResponse<OrderListResponse>> searchOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Searching orders with keyword: {}, userId: {}, status: {}, fromDate: {}, toDate: {}",
                keyword, userId, status, fromDate, toDate);

        OrderListRequest request = OrderListRequest.builder()
                .userId(userId)
                .status(status)
                .orderCode(keyword) // Use keyword as orderCode filter
                .fromDate(fromDate)
                .toDate(toDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<OrderListResponse> response = getOrderListUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thống kê đơn hàng của User (Admin/User)
     * GET /api/v1/orders/users/{userId}/statistics
     */
    @GetMapping("/users/{userId}/statistics")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long userId) {

        log.info("Getting statistics for userId: {}", userId);

        // Validate permission (Admin or Owner)
        Long userIdFromToken = jwtTokenService.extractUserId(jwt);
        String role = jwt.getClaimAsString("role");

        if (!"ADMIN".equalsIgnoreCase(role) && !userId.equals(userIdFromToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own statistics");
        }

        UserStatisticsResponse response = getUserStatisticsUseCase.execute(userId);
        return ResponseEntity.ok(response);
    }

    // ========== CUSTOMER ENDPOINTS ==========

    /**
     * CUSTOMER: Xem đơn hàng của mình
     * GET /api/v1/orders/my-orders
     */
    @GetMapping("/my-orders")
    public ResponseEntity<PageResponse<OrderListResponse>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Long userId = jwtTokenService.extractUserId(jwt);
        if (userId == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Cannot extract userId from token");
        }

        log.info("Getting orders for user: {} with filters - status: {}, orderCode: {}, fromDate: {}, toDate: {}",
                userId, status, orderCode, fromDate, toDate);

        OrderListRequest request = OrderListRequest.builder()
                .userId(userId)
                .status(status)
                .orderCode(orderCode)
                .fromDate(fromDate)
                .toDate(toDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<OrderListResponse> response = getOrderListUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    // ========== MERCHANT ENDPOINTS ==========

    /**
     * MERCHANT: Xem đơn hàng của mình
     * GET /api/v1/orders/merchants/me
     */
    @GetMapping("/merchants/me")
    public ResponseEntity<PageResponse<OrderListResponse>> getMyMerchantOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Long merchantId = jwtTokenService.extractUserId(jwt);
        if (merchantId == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Cannot extract merchantId from token");
        }

        log.info("Getting orders for merchant: {} with filters - status: {}, orderCode: {}, fromDate: {}, toDate: {}",
                merchantId, status, orderCode, fromDate, toDate);

        OrderListRequest request = OrderListRequest.builder()
                .status(status)
                .orderCode(orderCode)
                .fromDate(fromDate)
                .toDate(toDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<OrderListResponse> response = getMerchantOrdersUseCase.execute(merchantId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * MERCHANT: Xem chi tiết đơn hàng của mình
     * GET /api/v1/orders/merchants/me/{orderId}
     */
    @GetMapping("/merchants/me/{orderId}")
    public ResponseEntity<OrderDetailResponse> getMyMerchantOrderDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId) {

        Long merchantId = jwtTokenService.extractUserId(jwt);
        if (merchantId == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Cannot extract merchantId from token");
        }

        log.info("Merchant {} getting order detail for orderId: {}", merchantId, orderId);

        try {
            OrderDetailResponse response = getOrderDetailUseCase.execute(orderId);

            // Validate merchant ownership
            if (!response.getMerchantId().equals(merchantId)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                        "Order does not belong to this merchant");
            }

            return ResponseEntity.ok(response);
        } catch (OrderNotFoundException e) {
            log.warn("Order not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * MERCHANT: Cập nhật trạng thái đơn hàng (chỉ đơn có sản phẩm của merchant)
     * PUT /api/v1/orders/merchants/me/{orderId}/status
     */
    @PutMapping("/merchants/me/{orderId}/status")
    public ResponseEntity<OrderDetailResponse> updateMyMerchantOrderStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {

        Long merchantId = jwtTokenService.extractUserId(jwt);
        if (merchantId == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Cannot extract merchantId from token");
        }

        log.info("Merchant {} updating order status for orderId: {}, request: {}", merchantId, orderId, request);

        try {
            OrderDetailResponse response = updateOrderStatusUseCase.executeForMerchant(orderId, merchantId, request);
            return ResponseEntity.ok(response);
        } catch (OrderNotFoundException e) {
            log.warn("Order not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (MerchantOrderAccessDeniedException e) {
            log.warn("Merchant access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (OrderValidationException e) {
            log.warn("Order validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Request refund for an order
     * POST /api/v1/orders/{orderId}/refund
     * Supports: User (only their own orders), Admin (any order), Merchant (only
     * their merchant orders)
     */
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<RefundResponse> requestRefund(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId,
            @Valid @RequestBody RefundRequest request) {

        log.info("Refund request for orderId: {}, userId from token: {}", orderId,
                jwtTokenService.extractUserId(jwt));

        try {
            // Get order to validate ownership
            OrderDetailResponse orderDetail = getOrderDetailUseCase.execute(orderId);

            // Extract user info from JWT
            Long userIdFromToken = jwtTokenService.extractUserId(jwt);
            String role = jwt.getClaimAsString("role");

            if (userIdFromToken == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot extract userId from token");
            }

            // Validate ownership based on role
            if ("USER".equalsIgnoreCase(role)) {
                // User can only refund their own orders
                if (!orderDetail.getUserId().equals(userIdFromToken)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "You can only refund your own orders");
                }
            } else if ("MERCHANT".equalsIgnoreCase(role)) {
                // Merchant can only refund orders from their merchant
                if (!orderDetail.getMerchantId().equals(userIdFromToken)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "You can only refund orders from your merchant");
                }
            } else if (!"ADMIN".equalsIgnoreCase(role)) {
                // Admin can refund any order, other roles are not allowed
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only USER, MERCHANT, or ADMIN can request refunds");
            }

            // Process refund
            RefundResponse response = requestRefundUseCase.execute(orderId, request);
            return ResponseEntity.ok(response);

        } catch (OrderNotFoundException e) {
            log.warn("Order not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (OrderValidationException e) {
            log.warn("Order validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(RefundResponse.builder()
                    .orderId(orderId)
                    .status("ERROR")
                    .message(e.getMessage())
                    .build());
        } catch (ResponseStatusException e) {
            throw e; // Re-throw to preserve status code
        } catch (Exception e) {
            log.error("Unexpected error processing refund request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RefundResponse.builder()
                    .orderId(orderId)
                    .status("ERROR")
                    .message("Internal server error")
                    .build());
        }
    }

    /**
     * Health check endpoint
     * GET /api/v1/orders/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is running");
    }

    private boolean isMerchant(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        String role = jwt.getClaimAsString("role");
        return role != null && role.equalsIgnoreCase("MERCHANT");
    }
}
