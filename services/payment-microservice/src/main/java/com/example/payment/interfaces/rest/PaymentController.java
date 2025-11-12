package com.example.payment.interfaces.rest;

import com.example.payment.application.dto.PaymentRequest;
import com.example.payment.application.usecase.ProcessPaymentUseCase;
import com.example.payment.domain.exception.InvalidJwtTokenException;
import com.example.payment.infrastructure.security.JwtTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final JwtTokenService jwtTokenService;

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
}
