package com.example.order_service.infrastructure.service;

import com.example.order_service.application.dto.ProductValidationRequest;
import com.example.order_service.application.dto.ProductValidationResponse;
import com.example.order_service.application.dto.ProductValidationWrapper;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.repository.ProductServicePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class ProductServiceAdapter implements ProductServicePort {

    private final WebClient productWebClient;
    private final Executor executor = Executors.newCachedThreadPool();

    @Override
    public List<ProductValidationResponse> validateProducts(List<ProductValidationRequest> request) {
        // Gọi method async và block để trả về List (giữ nguyên interface)
        try {
            return validateProductsAsync(request).join();
        } catch (Exception e) {
            // Unwrap CompletionException nếu có
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException("Cannot connect to Product Service: " + e.getMessage(), e);
        }
    }

    /**
     * Method async để TimeLimiter hoạt động đúng
     * TimeLimiter yêu cầu method return CompletableFuture
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "validateProductsAsyncFallback")
    @TimeLimiter(name = "productService")
    public CompletableFuture<List<ProductValidationResponse>> validateProductsAsync(List<ProductValidationRequest> request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("=== CALLING PRODUCT SERVICE ===");
                log.info("Endpoint: /validate");
                log.info("Request body: {}", request);

                // Lấy authentication từ SecurityContext
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                
                // Kiểm tra authentication có phải JwtAuthenticationToken không
                if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
                    log.error("Authentication is null or not a JwtAuthenticationToken");
                    throw new RuntimeException("Authentication không hợp lệ");
                }
                
                JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
                String jwtToken = jwtAuth.getToken().getTokenValue();

                // Gọi Product Service và nhận wrapper response
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
            List<ProductValidationRequest> request, Exception ex) {
        log.error("Circuit Breaker fallback triggered for product validation. Error: {}", ex.getMessage());
        log.warn("Product Service unavailable - rejecting order creation");
        return CompletableFuture.failedFuture(
                new OrderValidationException("Không thể xác thực sản phẩm, vui lòng thử lại sau")
        );
    }
}