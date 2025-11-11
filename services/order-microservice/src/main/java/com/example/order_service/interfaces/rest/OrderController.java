package com.example.order_service.interfaces.rest;

import com.example.order_service.application.dto.CreateOrderRequest;
import com.example.order_service.application.dto.OrderResponse;
import com.example.order_service.application.usecase.CreateOrderUseCase;
import com.example.order_service.domain.exception.InvalidJwtTokenException;
import com.example.order_service.infrastructure.security.JwtTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
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
                 roleClaim.equalsIgnoreCase("ORDER_CREATE"));

        if (!hasScope && !hasRolePermission) {
            log.error("Missing required scope 'order:create'. Available scopes: {}, roleClaim: {}", scopes, roleClaim);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Thiếu quyền order:create");
        }
        
        // 5. Extract jti for audit logging
        String jti = jwt.getId();
        log.info("JWT jti (audit): {}", jti);
        
//  THÊM LOGGING ĐỂ DEBUG
        log.info("=== CREATE ORDER REQUEST ===");
        log.info("UserId from token: {}", request.getUserId());
        log.info("Discount: {}", request.getDiscount());
        log.info("ShippingFee: {}", request.getShippingFee());
        log.info("Note: {}", request.getNote());
        log.info("Number of items: {}", request.getOrderItems() != null ? request.getOrderItems().size() : 0);

        if (request.getOrderItems() != null) {
            request.getOrderItems().forEach(item ->
                    log.info("OrderItem: productId={}, quantity={} (productName and unitPrice will be fetched from Product Service)",
                            item.getProductId(),
                            item.getQuantity())
            );
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
     * Health check endpoint
     * GET /api/v1/orders/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is running");
    }
}
