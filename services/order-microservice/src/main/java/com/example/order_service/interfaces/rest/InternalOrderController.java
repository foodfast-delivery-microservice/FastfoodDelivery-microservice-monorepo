package com.example.order_service.interfaces.rest;

import com.example.order_service.application.dto.OrderDetailResponse;
import com.example.order_service.application.usecase.GetOrderDetailUseCase;
import com.example.order_service.domain.exception.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API controller for service-to-service calls
 * These endpoints do not require authentication and are only for internal
 * microservice communication
 */
@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
@Slf4j
public class InternalOrderController {

    private final GetOrderDetailUseCase getOrderDetailUseCase;
    private final com.example.order_service.application.usecase.CheckMerchantOrdersUseCase checkMerchantOrdersUseCase;

    /**
     * Check if merchant can be deleted (all orders must be DELIVERED or CANCELLED)
     * GET /api/internal/orders/merchant/{merchantId}/can-delete
     * 
     * This endpoint is used by user-service to validate merchant deletion
     */
    @GetMapping("/merchant/{merchantId}/can-delete")
    public ResponseEntity<com.example.order_service.application.usecase.CheckMerchantOrdersUseCase.MerchantOrderCheckResult> checkMerchantCanBeDeleted(
            @PathVariable Long merchantId) {
        log.info("Internal API: Checking if merchant {} can be deleted", merchantId);

        try {
            var result = checkMerchantOrdersUseCase.execute(merchantId);
            log.info("Internal API: Merchant {} deletion check result: canDelete={}, activeOrders={}",
                    merchantId, result.isCanDelete(), result.getActiveOrderCount());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Internal API: Error checking merchant deletion for merchantId: {}", merchantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get order detail by order ID (Internal API - no authentication required)
     * GET /api/internal/orders/{orderId}
     * 
     * This endpoint is used by other microservices (e.g., payment-service) to get
     * order information
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetailInternal(@PathVariable Long orderId) {
        log.info("Internal API: Getting order detail for orderId: {}", orderId);

        try {
            OrderDetailResponse response = getOrderDetailUseCase.execute(orderId);
            log.info("Internal API: Order detail retrieved successfully for orderId: {}", orderId);
            return ResponseEntity.ok(response);
        } catch (OrderNotFoundException e) {
            log.warn("Internal API: Order not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Internal API: Error getting order detail for orderId: {}", orderId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
