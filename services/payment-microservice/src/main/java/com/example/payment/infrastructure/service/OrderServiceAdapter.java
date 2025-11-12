package com.example.payment.infrastructure.service;

import com.example.payment.domain.port.OrderServicePort;
import com.example.payment.infrastructure.client.dto.OrderDetailResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
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

@Slf4j
@Service
public class OrderServiceAdapter implements OrderServicePort {

    private final WebClient orderWebClient;
    private final ObjectMapper objectMapper;
    private final Executor executor = Executors.newCachedThreadPool();
    private OrderServiceAdapter self; // Self reference để gọi qua proxy
    
    // Constructor injection
    public OrderServiceAdapter(WebClient orderWebClient, ObjectMapper objectMapper) {
        this.orderWebClient = orderWebClient;
        this.objectMapper = objectMapper;
    }
    
    // Setter injection cho self reference (được gọi sau khi bean được tạo)
    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(OrderServiceAdapter self) {
        this.self = self;
    }

    @Override
    public OrderDetailResponse getOrderDetail(Long orderId) {
        // Capture JWT token trước khi vào async thread (fix SecurityContext issue)
        String jwtToken = getJwtToken();
        
        // Gọi method async qua proxy để annotations hoạt động đúng
        try {
            if (self != null) {
                return self.getOrderDetailAsync(orderId, jwtToken).join();
            } else {
                // Fallback: gọi trực tiếp nếu self chưa được inject
                return getOrderDetailAsync(orderId, jwtToken).join();
            }
        } catch (Exception e) {
            // Unwrap CompletionException nếu có
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException("Cannot connect to Order Service: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lấy JWT token từ SecurityContext (phải gọi trước khi vào async thread)
     */
    private String getJwtToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            log.error("Authentication is null or not a JwtAuthenticationToken");
            throw new RuntimeException("Authentication không hợp lệ - không thể forward JWT token");
        }
        
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        return jwtAuth.getToken().getTokenValue();
    }

    /**
     * Method async để TimeLimiter hoạt động đúng
     * TimeLimiter yêu cầu method return CompletableFuture
     * JWT token được truyền vào để tránh mất SecurityContext trong async thread
     */
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrderDetailAsyncFallback")
    @TimeLimiter(name = "orderService")
    public CompletableFuture<OrderDetailResponse> getOrderDetailAsync(
            Long orderId, String jwtToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("=== CALLING ORDER SERVICE ===");
                log.info("Endpoint: GET /api/v1/orders/{}", orderId);

                String responseJson = orderWebClient.get()
                        .uri("/{orderId}", orderId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(error -> log.error("Error calling Order Service: {}", error.getMessage()))
                        .block();

                if (responseJson == null) {
                    log.error("Order Service returned null response");
                    throw new RuntimeException("Order Service returned invalid response");
                }

                log.info("=== ORDER SERVICE RESPONSE ===");
                log.info("Response received for orderId: {}", orderId);

                // Parse response (OrderDetailResponse is returned directly, not wrapped in ApiResponse)
                OrderDetailResponse orderDetail = objectMapper.readValue(responseJson, OrderDetailResponse.class);

                log.info("Order detail retrieved: orderId={}, userId={}, status={}", 
                        orderDetail.getId(), orderDetail.getUserId(), orderDetail.getStatus());

                return orderDetail;

            } catch (WebClientResponseException.NotFound ex) {
                log.warn("Order {} not found (404)", orderId);
                throw new RuntimeException("Order not found with id: " + orderId);
            } catch (WebClientResponseException.Forbidden ex) {
                // 403 Forbidden là lỗi authorization, không phải service unavailable
                // Không nên trigger Circuit Breaker với lỗi này
                log.error("Access forbidden when calling order service (403). OrderId: {}, Message: {}", 
                        orderId, ex.getMessage());
                throw new RuntimeException("Không có quyền truy cập Order Service. Vui lòng kiểm tra token và quyền truy cập.");
            } catch (WebClientResponseException.Unauthorized ex) {
                // 401 Unauthorized là lỗi authentication
                log.error("Auth failed when calling order service (401)", ex);
                throw new RuntimeException("Token không hợp lệ hoặc đã hết hạn");
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                // Các lỗi HTTP khác (500, 503, etc.) - có thể là service unavailable
                // KHÔNG wrap exception - để Circuit Breaker có thể thấy exception gốc
                log.error("HTTP error when calling Order Service: {} - {}", e.getStatusCode(), e.getMessage(), e);
                throw e; // Re-throw exception gốc để Circuit Breaker có thể record
            } catch (org.springframework.web.reactive.function.client.WebClientException e) {
                // Network errors, timeouts - service unavailable
                // KHÔNG wrap exception - để Circuit Breaker có thể thấy exception gốc
                log.error("WebClient exception when calling Order Service (network/timeout)", e);
                throw e; // Re-throw exception gốc để Circuit Breaker có thể record
            } catch (JsonProcessingException e) {
                // JSON parsing errors (includes JsonMappingException)
                log.error("Failed to parse Order Service response JSON", e);
                throw new RuntimeException("Failed to parse Order Service response: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error calling Order Service", e);
                throw new RuntimeException("Unexpected error calling Order Service: " + e.getMessage(), e);
            }
        }, executor);
    }

    /**
     * Fallback method cho async method khi Circuit Breaker mở hoặc timeout
     */
    public CompletableFuture<OrderDetailResponse> getOrderDetailAsyncFallback(
            Long orderId, String jwtToken, Exception ex) {
        log.error("Circuit Breaker fallback triggered for order detail. Error: {}", ex.getMessage());
        log.warn("Order Service unavailable - rejecting payment processing");
        return CompletableFuture.failedFuture(
                new RuntimeException("Không thể lấy thông tin đơn hàng, vui lòng thử lại sau")
        );
    }
}

