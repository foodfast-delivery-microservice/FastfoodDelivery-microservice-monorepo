package com.example.order_service.interfaces.rest;

import com.example.order_service.application.dto.*;
import com.example.order_service.application.usecase.GetMerchantOrdersUseCase;
import com.example.order_service.application.usecase.GetOrderDetailUseCase;
import com.example.order_service.application.usecase.GetOrderListUseCase;
import com.example.order_service.application.usecase.UpdateOrderStatusUseCase;
import com.example.order_service.domain.exception.MerchantOrderAccessDeniedException;
import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.infrastructure.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderManagementController {

    private final GetOrderListUseCase getOrderListUseCase;
    private final GetOrderDetailUseCase getOrderDetailUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final GetMerchantOrdersUseCase getMerchantOrdersUseCase;
    private final JwtTokenService jwtTokenService;

    /**
     * Lấy danh sách đơn hàng
     * GET /api/v1/orders
     */
    @GetMapping
    public ResponseEntity<PageResponse<OrderListResponse>> getOrderList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Getting order list with filters - userId: {}, status: {}, orderCode: {}, fromDate: {}, toDate: {}, page: {}, size: {}",
                userId, status, orderCode, fromDate, toDate, page, size);

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

    /**
     * Lấy chi tiết đơn hàng
     * GET /api/v1/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(@PathVariable Long orderId) {
        log.info("Getting order detail for orderId: {}", orderId);

        try {
            OrderDetailResponse response = getOrderDetailUseCase.execute(orderId);
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
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Cannot extract merchantId from token");
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
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Cannot extract merchantId from token");
        }

        log.info("Merchant {} getting order detail for orderId: {}", merchantId, orderId);

        try {
            OrderDetailResponse response = getOrderDetailUseCase.execute(orderId);
            
            // Validate merchant ownership
            if (!response.getMerchantId().equals(merchantId)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Order does not belong to this merchant");
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
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Cannot extract merchantId from token");
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

    private boolean isMerchant(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        String role = jwt.getClaimAsString("role");
        return role != null && role.equalsIgnoreCase("MERCHANT");
    }

}
