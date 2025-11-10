package com.example.order_service.interfaces.rest;

import com.example.order_service.application.dto.CreateOrderRequest;
import com.example.order_service.application.dto.OrderResponse;
import com.example.order_service.application.usecase.CreateOrderUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    /**
     * Tạo đơn hàng mới
     * POST /api/v1/orders
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
//  THÊM LOGGING ĐỂ DEBUG
        log.info("=== CREATE ORDER REQUEST ===");
        log.info("UserId: {}", request.getUserId());
        log.info("Discount: {}", request.getDiscount());
        log.info("ShippingFee: {}", request.getShippingFee());
        log.info("Note: {}", request.getNote());
        log.info("Number of items: {}", request.getOrderItems() != null ? request.getOrderItems().size() : 0);

        if (request.getOrderItems() != null) {
            request.getOrderItems().forEach(item ->
                    log.info("OrderItem: productId={}, name={}, price={}, qty={}",
                            item.getProductId(),
                            item.getProductName(),
                            item.getUnitPrice(),
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
        log.info("Creating order for user: {}", request.getUserId());
        OrderResponse response = createOrderUseCase.execute(request, idempotencyKey);
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
