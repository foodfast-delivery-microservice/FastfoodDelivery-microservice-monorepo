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

        log.info("Idempotency-Key: {}", idempotencyKey);
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
