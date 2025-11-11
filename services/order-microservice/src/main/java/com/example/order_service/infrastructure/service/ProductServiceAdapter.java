package com.example.order_service.infrastructure.service;

import com.example.order_service.application.dto.ProductValidationRequest;
import com.example.order_service.application.dto.ProductValidationResponse;
import com.example.order_service.application.dto.ProductValidationWrapper;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.repository.ProductServicePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class ProductServiceAdapter implements ProductServicePort {

    private final WebClient productWebClient;
    private final Executor executor = Executors.newCachedThreadPool();
    private ProductServiceAdapter self; // Self reference để gọi qua proxy
    
    // Constructor injection
    public ProductServiceAdapter(WebClient productWebClient) {
        this.productWebClient = productWebClient;
    }
    
    // Setter injection cho self reference (được gọi sau khi bean được tạo)
    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(ProductServiceAdapter self) {
        this.self = self;
    }

    @Override
    public List<ProductValidationResponse> validateProducts(List<ProductValidationRequest> request) {
        // Capture JWT token trước khi vào async thread (fix SecurityContext issue)
        String jwtToken = getJwtToken();
        
        // Gọi method async qua proxy để annotations hoạt động đúng
        // Nếu self chưa được inject, fallback về gọi trực tiếp (không lý tưởng nhưng vẫn hoạt động)
        try {
            if (self != null) {
                return self.validateProductsAsync(request, jwtToken).join();
            } else {
                // Fallback: gọi trực tiếp nếu self chưa được inject
                return validateProductsAsync(request, jwtToken).join();
            }
        } catch (Exception e) {
            // Unwrap CompletionException nếu có
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            if (e.getCause() instanceof OrderValidationException) {
                throw (OrderValidationException) e.getCause();
            }
            throw new RuntimeException("Cannot connect to Product Service: " + e.getMessage(), e);
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
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "validateProductsAsyncFallback")
    @TimeLimiter(name = "productService")
    public CompletableFuture<List<ProductValidationResponse>> validateProductsAsync(
            List<ProductValidationRequest> request, String jwtToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("=== CALLING PRODUCT SERVICE ===");
                log.info("Endpoint: /validate");
                log.info("Request body: {}", request);

                // Gọi Product Service và nhận wrapper response
                // JWT token đã được capture trước khi vào async thread
                ProductValidationWrapper wrapper = productWebClient.post()
                        .uri("/validate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(ProductValidationWrapper.class)
                        .doOnError(error -> log.error("Error calling Product Service: {}", error.getMessage()))
                        .block();

                // Kiểm tra wrapper
                if (wrapper == null || wrapper.getData() == null) {
                    log.error("Product Service returned null response");
                    throw new RuntimeException("Product Service returned invalid response");
                }

                log.info("=== PRODUCT SERVICE RESPONSE ===");
                log.info("Status: {}", wrapper.getStatus());
                log.info("Message: {}", wrapper.getMessage());
                log.info("Number of products: {}", wrapper.getData().size());

                // Log chi tiết từng product
                wrapper.getData().forEach(p ->
                        log.info("Product: id={}, name={}, price={}, success={}",
                                p.productId(), p.productName(), p.unitPrice(), p.success())
                );

                return wrapper.getData();

            } catch (WebClientResponseException.Unauthorized ex) {
                log.error("Auth failed when calling product service", ex);
                throw new RuntimeException("Authorization failed", ex);
            } catch (org.springframework.web.reactive.function.client.WebClientException e) {
                log.error("WebClient exception when calling Product Service", e);
                throw new RuntimeException("Cannot connect to Product Service: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Error calling Product Service", e);
                throw new RuntimeException("Cannot connect to Product Service: " + e.getMessage(), e);
            }
        }, executor);
    }

    /**
     * Fallback method cho async method khi Circuit Breaker mở hoặc timeout
     * Không cho phép tạo đơn khi chưa xác thực được sản phẩm
     */
    public CompletableFuture<List<ProductValidationResponse>> validateProductsAsyncFallback(
            List<ProductValidationRequest> request, String jwtToken, Exception ex) {
        log.error("Circuit Breaker fallback triggered for product validation. Error: {}", ex.getMessage());
        log.warn("Product Service unavailable - rejecting order creation");
        return CompletableFuture.failedFuture(
                new OrderValidationException("Không thể xác thực sản phẩm, vui lòng thử lại sau")
        );
    }
}