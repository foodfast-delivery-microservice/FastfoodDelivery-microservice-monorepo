package com.example.order_service.infrastructure.service;

import com.example.order_service.application.dto.PaymentValidationResponse;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.repository.PaymentServicePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class PaymentServiceAdapter implements PaymentServicePort {

    private final Executor executor = Executors.newCachedThreadPool();
    private PaymentServiceAdapter self; // Self reference để gọi qua proxy
    
    // Constructor injection
    public PaymentServiceAdapter() {
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

                // For now, validate locally since Payment Service doesn't have validation endpoint
                // Allowed payment methods
                String[] allowedMethods = {"COD", "CARD", "WALLET", "BANK_TRANSFER"};
                boolean isValid = false;
                for (String method : allowedMethods) {
                    if (method.equalsIgnoreCase(paymentMethod)) {
                        isValid = true;
                        break;
                    }
                }

                if (!isValid) {
                    log.warn("Invalid payment method: {}", paymentMethod);
                    return new PaymentValidationResponse(userId, paymentMethod, false, 
                            "Payment method không hợp lệ: " + paymentMethod);
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
        
        // Fallback to local validation
        String[] allowedMethods = {"COD", "CARD", "WALLET", "BANK_TRANSFER"};
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
                            "Payment method không hợp lệ: " + paymentMethod));
        }
        
        return CompletableFuture.completedFuture(
                new PaymentValidationResponse(userId, paymentMethod, true, null));
    }
}

