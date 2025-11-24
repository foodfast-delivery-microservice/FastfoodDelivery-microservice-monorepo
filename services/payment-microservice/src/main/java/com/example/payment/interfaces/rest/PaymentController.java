package com.example.payment.interfaces.rest;

import com.example.payment.application.dto.PageResponse;
import com.example.payment.application.dto.PaymentListRequest;
import com.example.payment.application.dto.PaymentRequest;
import com.example.payment.application.dto.PaymentResponse;
import com.example.payment.application.dto.PaymentStatisticsResponse;
import com.example.payment.application.usecase.GetMerchantPaymentStatisticsUseCase;
import com.example.payment.application.usecase.GetMerchantPaymentsUseCase;
import com.example.payment.application.usecase.ProcessPaymentUseCase;
import com.example.payment.domain.exception.InvalidJwtTokenException;
import com.example.payment.infrastructure.security.JwtTokenService;
import jakarta.validation.Valid;
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

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final JwtTokenService jwtTokenService;
    private final GetMerchantPaymentsUseCase getMerchantPaymentsUseCase;
    private final GetMerchantPaymentStatisticsUseCase getMerchantPaymentStatisticsUseCase;
    private final com.example.payment.domain.repository.PaymentRepository paymentRepository;

    @PostMapping
    public ResponseEntity<?> processPayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PaymentRequest request) {
        
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
        boolean hasScope = scopes != null && scopes.contains("payment:process");
        boolean hasRolePermission = roleClaim != null &&
                (roleClaim.equalsIgnoreCase("USER") ||
                 roleClaim.equalsIgnoreCase("ADMIN") ||
                 roleClaim.equalsIgnoreCase("PAYMENT_PROCESS"));

        if (!hasScope && !hasRolePermission) {
            log.error("Missing required scope 'payment:process'. Available scopes: {}, roleClaim: {}", scopes, roleClaim);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Thiếu quyền payment:process");
        }
        
        log.info("Processing payment for orderId: {}, userId: {}", request.getOrderId(), request.getUserId());
        
        // Execute use case to process payment
        boolean success = processPaymentUseCase.execute(request);
        
        if (success) {
            log.info("✓ Payment processed successfully for orderId: {}, userId: {}", 
                    request.getOrderId(), request.getUserId());
            return ResponseEntity.ok("Payment processed successfully");
        } else {
            log.warn("Payment failed for orderId: {}, userId: {}", 
                    request.getOrderId(), request.getUserId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment failed");
        }
    }

    // ========== MERCHANT ENDPOINTS ==========

    /**
     * MERCHANT: Lấy danh sách payments của merchant
     * GET /api/v1/payments/merchants/me
     */
    @GetMapping("/merchants/me")
    public ResponseEntity<PageResponse<PaymentResponse>> getMyMerchantPayments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "DESC") String sortDirection,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        Long merchantId = jwtTokenService.extractUserId(jwt);
        if (merchantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot extract merchantId from token");
        }

        log.info("Merchant {} getting payments list", merchantId);

        PaymentListRequest request = PaymentListRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .status(status)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        PageResponse<PaymentResponse> response = getMerchantPaymentsUseCase.execute(merchantId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * MERCHANT: Lấy thống kê doanh thu
     * GET /api/v1/payments/merchants/me/statistics
     */
    @GetMapping("/merchants/me/statistics")
    public ResponseEntity<PaymentStatisticsResponse> getMyMerchantPaymentStatistics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        Long merchantId = jwtTokenService.extractUserId(jwt);
        if (merchantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot extract merchantId from token");
        }

        log.info("Merchant {} getting payment statistics", merchantId);

        PaymentStatisticsResponse response = getMerchantPaymentStatisticsUseCase.execute(merchantId, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by orderId (for internal use by Order Service)
     * GET /api/v1/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        log.info("Getting payment for orderId: {}", orderId);
        
        return paymentRepository.findByOrderId(orderId)
                .map(payment -> {
                    PaymentResponse response = PaymentResponse.builder()
                            .id(payment.getId())
                            .orderId(payment.getOrderId())
                            .userId(payment.getUserId())
                            .merchantId(payment.getMerchantId())
                            .amount(payment.getAmount())
                            .currency(payment.getCurrency())
                            .status(payment.getStatus().toString())
                            .transactionNo(payment.getTransactionNo())
                            .failReason(payment.getFailReason())
                            .timestamp(payment.getCreatedAt())
                            .build();
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
