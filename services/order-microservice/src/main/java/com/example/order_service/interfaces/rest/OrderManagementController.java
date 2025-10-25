package com.example.order_service.interfaces.rest;

import com.example.order_service.application.dto.*;
import com.example.order_service.application.usecase.GetOrderDetailUseCase;
import com.example.order_service.application.usecase.GetOrderListUseCase;
import com.example.order_service.application.usecase.UpdateOrderStatusUseCase;
import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.exception.OrderValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderManagementController {

    private final GetOrderListUseCase getOrderListUseCase;
    private final GetOrderDetailUseCase getOrderDetailUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

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
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {

        log.info("Updating order status for orderId: {}, request: {}", orderId, request);

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

}
