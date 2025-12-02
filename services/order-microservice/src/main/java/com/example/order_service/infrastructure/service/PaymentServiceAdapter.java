package com.example.order_service.infrastructure.service;

import com.example.order_service.application.dto.PaymentInfo;
import com.example.order_service.application.dto.PaymentValidationResponse;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.repository.PaymentServicePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class PaymentServiceAdapter implements PaymentServicePort {

    private final WebClient paymentWebClient;
    private final ObjectMapper objectMapper;
    private final Executor executor = Executors.newCachedThreadPool();
    private PaymentServiceAdapter self; // Self reference để gọi qua proxy
    
    // Constructor injection
    public PaymentServiceAdapter(WebClient paymentWebClient, ObjectMapper objectMapper) {
        this.paymentWebClient = paymentWebClient;
        this.objectMapper = objectMapper;
    }
    
    // Setter injection cho self reference (được gọi sau khi bean được tạo)
    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(PaymentServiceAdapter self) {
        this.self = self;
    }

    @Override
    public PaymentValidationResponse validatePaymentMethod(Long userId, String paymentMethod) {
        // Capture JWT token trước khi vào async thread (fix SecurityContext issue)
        String jwtToken = getJwtToken();
        
        // Gọi method async qua proxy để annotations hoạt động đúng
        try {
            if (self != null) {
                return self.validatePaymentMethodAsync(userId, paymentMethod, jwtToken).join();
            } else {
                // Fallback: gọi trực tiếp nếu self chưa được inject
                return validatePaymentMethodAsync(userId, paymentMethod, jwtToken).join();
            }
        } catch (Exception e) {
            // Unwrap CompletionException nếu có
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            if (e.getCause() instanceof OrderValidationException) {
                throw (OrderValidationException) e.getCause();
            }
            throw new RuntimeException("Cannot connect to Payment Service: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lấy JWT token từ SecurityContext (phải gọi trước khi vào async thread)
     */
    private String getJwtToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            log.error("Authentication is null or not a JwtAuthenticationToken");
            throw new RuntimeException("Authentication không hợp lệ");
        }
        
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        return jwtAuth.getToken().getTokenValue();
    }

    /**
     * Method async để TimeLimiter hoạt động đúng
     * TimeLimiter yêu cầu method return CompletableFuture
     * JWT token được truyền vào để tránh mất SecurityContext trong async thread
     * 
     * Note: Payment Service may not have a validation endpoint yet.
     * For now, we'll validate locally (check if paymentMethod is in allowed list)
     * In future, can call Payment Service endpoint if available
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "validatePaymentMethodAsyncFallback")
    @TimeLimiter(name = "paymentService")
    public CompletableFuture<PaymentValidationResponse> validatePaymentMethodAsync(
            Long userId, String paymentMethod, String jwtToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("=== VALIDATING PAYMENT METHOD ===");
                log.info("UserId: {}, PaymentMethod: {}", userId, paymentMethod);

                // Chỉ cho phép thanh toán bằng QR (duy nhất phương thức thanh toán)
                String[] allowedMethods = {"QR", "QR_CODE", "QRCODE"};
                boolean isValid = false;
                for (String method : allowedMethods) {
                    if (method.equalsIgnoreCase(paymentMethod)) {
                        isValid = true;
                        break;
                    }
                }

                if (!isValid) {
                    log.warn("Invalid payment method: {}. Chỉ chấp nhận thanh toán bằng QR", paymentMethod);
                    return new PaymentValidationResponse(userId, paymentMethod, false, 
                            "Phương thức thanh toán không hợp lệ. Hệ thống chỉ chấp nhận thanh toán bằng QR. Payment method: " + paymentMethod);
                }

                log.info("✓ Payment method {} validated successfully for user {}", paymentMethod, userId);
                return new PaymentValidationResponse(userId, paymentMethod, true, null);

            } catch (Exception e) {
                log.error("Error validating payment method", e);
                throw new RuntimeException("Cannot validate payment method: " + e.getMessage(), e);
            }
        }, executor);
    }

    /**
     * Fallback method cho async method khi Circuit Breaker mở hoặc timeout
     * For payment validation, we can allow it to proceed with local validation
     */
    public CompletableFuture<PaymentValidationResponse> validatePaymentMethodAsyncFallback(
            Long userId, String paymentMethod, String jwtToken, Exception ex) {
        log.error("Circuit Breaker fallback triggered for payment validation. Error: {}", ex.getMessage());
        log.warn("Payment Service unavailable - using local validation");
        
        // Fallback to local validation - Chỉ cho phép QR
        String[] allowedMethods = {"QR", "QR_CODE", "QRCODE"};
        boolean isValid = false;
        for (String method : allowedMethods) {
            if (method.equalsIgnoreCase(paymentMethod)) {
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            return CompletableFuture.completedFuture(
                    new PaymentValidationResponse(userId, paymentMethod, false, 
                            "Phương thức thanh toán không hợp lệ. Hệ thống chỉ chấp nhận thanh toán bằng QR. Payment method: " + paymentMethod));
        }
        
        return CompletableFuture.completedFuture(
                new PaymentValidationResponse(userId, paymentMethod, true, null));
    }

    @Override
    public PaymentInfo getPaymentByOrderId(Long orderId) {
        // Capture JWT token trước khi vào async thread
        String jwtToken = getJwtToken();
        
        // Gọi method async qua proxy để annotations hoạt động đúng
        try {
            if (self != null) {
                return self.getPaymentByOrderIdAsync(orderId, jwtToken).join();
            } else {
                // Fallback: gọi trực tiếp nếu self chưa được inject
                return getPaymentByOrderIdAsync(orderId, jwtToken).join();
            }
        } catch (Exception e) {
            // Unwrap CompletionException nếu có
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException("Cannot get payment from Payment Service: " + e.getMessage(), e);
        }
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "getPaymentByOrderIdAsyncFallback")
    @TimeLimiter(name = "paymentService")
    public CompletableFuture<PaymentInfo> getPaymentByOrderIdAsync(Long orderId, String jwtToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("=== CALLING PAYMENT SERVICE ===");
                log.info("Endpoint: GET /api/v1/payments/order/{}", orderId);

                String responseJson = paymentWebClient.get()
                        .uri("/order/{orderId}", orderId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(error -> log.error("Error calling Payment Service: {}", error.getMessage()))
                        .block();

                if (responseJson == null) {
                    log.error("Payment Service returned null response for orderId: {}", orderId);
                    throw new RuntimeException("Payment not found for orderId: " + orderId);
                }

                log.info("=== PAYMENT SERVICE RESPONSE ===");
                log.info("Response received for orderId: {}", orderId);

                // Parse response to PaymentResponse DTO (from Payment Service)
                PaymentResponseDto paymentResponse = objectMapper.readValue(responseJson, PaymentResponseDto.class);

                log.info("Payment retrieved: paymentId={}, orderId={}, status={}", 
                        paymentResponse.getId(), paymentResponse.getOrderId(), paymentResponse.getStatus());

                // Convert to PaymentInfo
                return PaymentInfo.builder()
                        .paymentId(paymentResponse.getId())
                        .orderId(paymentResponse.getOrderId())
                        .amount(paymentResponse.getAmount())
                        .status(paymentResponse.getStatus())
                        .build();

            } catch (WebClientResponseException.NotFound ex) {
                log.warn("Payment for order {} not found (404)", orderId);
                throw new RuntimeException("Payment not found for orderId: " + orderId);
            } catch (WebClientResponseException.Forbidden ex) {
                log.error("Forbidden (403) when getting payment for order {}", orderId);
                throw new RuntimeException("Access denied when getting payment for orderId: " + orderId);
            } catch (WebClientResponseException ex) {
                log.error("Payment Service returned error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                throw new RuntimeException("Payment Service error: " + ex.getMessage());
            } catch (Exception e) {
                log.error("Error getting payment from Payment Service", e);
                throw new RuntimeException("Cannot get payment from Payment Service: " + e.getMessage(), e);
            }
        }, executor);
    }

    public CompletableFuture<PaymentInfo> getPaymentByOrderIdAsyncFallback(
            Long orderId, String jwtToken, Exception ex) {
        log.error("Circuit Breaker fallback triggered for getPaymentByOrderId. Error: {}", ex.getMessage());
        throw new RuntimeException("Payment Service unavailable: " + ex.getMessage(), ex);
    }

    // DTO class to match Payment Service response
    private static class PaymentResponseDto {
        private Long id;
        private Long orderId;
        private java.math.BigDecimal amount;
        private String status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}

