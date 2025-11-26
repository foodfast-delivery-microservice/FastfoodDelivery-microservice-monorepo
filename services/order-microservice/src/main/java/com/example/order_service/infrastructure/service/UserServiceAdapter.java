package com.example.order_service.infrastructure.service;

import com.example.order_service.application.dto.UserValidationResponse;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.repository.UserServicePort;
import com.fasterxml.jackson.databind.JsonNode;
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
public class UserServiceAdapter implements UserServicePort {

    private final WebClient userWebClient;
    private final ObjectMapper objectMapper;
    private final Executor executor = Executors.newCachedThreadPool();
    private UserServiceAdapter self; // Self reference để gọi qua proxy
    
    // Constructor injection
    public UserServiceAdapter(WebClient userWebClient, ObjectMapper objectMapper) {
        this.userWebClient = userWebClient;
        this.objectMapper = objectMapper;
    }
    
    // Setter injection cho self reference (được gọi sau khi bean được tạo)
    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(UserServiceAdapter self) {
        this.self = self;
    }

    @Override
    public UserValidationResponse validateUser(Long userId) {
        // Capture JWT token trước khi vào async thread (fix SecurityContext issue)
        String jwtToken = getJwtToken();
        
        // Gọi method async qua proxy để annotations hoạt động đúng
        try {
            if (self != null) {
                return self.validateUserAsync(userId, jwtToken).join();
            } else {
                // Fallback: gọi trực tiếp nếu self chưa được inject
                return validateUserAsync(userId, jwtToken).join();
            }
        } catch (Exception e) {
            // Unwrap CompletionException nếu có
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            if (e.getCause() instanceof OrderValidationException) {
                throw (OrderValidationException) e.getCause();
            }
            throw new RuntimeException("Cannot connect to User Service: " + e.getMessage(), e);
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
    @CircuitBreaker(name = "userService", fallbackMethod = "validateUserAsyncFallback")
    @TimeLimiter(name = "userService")
    public CompletableFuture<UserValidationResponse> validateUserAsync(
            Long userId, String jwtToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("=== CALLING USER SERVICE ===");
                log.info("Endpoint: GET /api/v1/users/{}/validate", userId);

                // Gọi User Service GET /api/v1/users/{id}/validate (endpoint cho phép USER role)
                // Response format: ApiResponse<CreateUserResponse>
                String responseJson = userWebClient.get()
                        .uri("/{id}/validate", userId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(error -> log.error("Error calling User Service: {}", error.getMessage()))
                        .block();

                if (responseJson == null) {
                    log.error("User Service returned null response");
                    throw new RuntimeException("User Service returned invalid response");
                }

                log.info("=== USER SERVICE RESPONSE ===");
                log.info("Response: {}", responseJson);

                // Parse ApiResponse wrapper
                JsonNode rootNode = objectMapper.readTree(responseJson);
                JsonNode dataNode = rootNode.get("data");
                
                if (dataNode == null || dataNode.isNull()) {
                    // User not found
                    log.warn("User {} not found", userId);
                    return new UserValidationResponse(userId, false, false, null);
                }

                // Extract user data
                Long id = dataNode.has("id") ? dataNode.get("id").asLong() : userId;
                String username = dataNode.has("username") ? dataNode.get("username").asText() : null;
                boolean active = !dataNode.has("active") || dataNode.get("active").asBoolean();
                boolean exists = true;

                log.info("User validation result: userId={}, exists={}, active={}, username={}",
                        id, exists, active, username);

                return new UserValidationResponse(id, exists, active, username);

            } catch (WebClientResponseException.NotFound ex) {
                log.warn("User {} not found (404)", userId);
                return new UserValidationResponse(userId, false, false, null);
            } catch (WebClientResponseException.Forbidden ex) {
                // 403 Forbidden là lỗi authorization, không phải service unavailable
                // Không nên trigger Circuit Breaker với lỗi này
                log.error("Access forbidden when calling user service (403). UserId: {}, Message: {}", 
                        userId, ex.getMessage());
                throw new OrderValidationException(
                        "Không có quyền truy cập User Service. Vui lòng kiểm tra token và quyền truy cập."
                );
            } catch (WebClientResponseException.Unauthorized ex) {
                // 401 Unauthorized là lỗi authentication
                log.error("Auth failed when calling user service (401)", ex);
                throw new OrderValidationException("Token không hợp lệ hoặc đã hết hạn");
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                // Các lỗi HTTP khác (500, 503, etc.) - có thể là service unavailable
                log.error("HTTP error when calling User Service: {} - {}", e.getStatusCode(), e.getMessage(), e);
                throw new RuntimeException("User Service error: " + e.getStatusCode() + " - " + e.getMessage(), e);
            } catch (org.springframework.web.reactive.function.client.WebClientException e) {
                // Network errors, timeouts - service unavailable
                log.error("WebClient exception when calling User Service (network/timeout)", e);
                throw new RuntimeException("Cannot connect to User Service: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error calling User Service", e);
                throw new RuntimeException("Cannot connect to User Service: " + e.getMessage(), e);
            }
        }, executor);
    }

    /**
     * Fallback method cho async method khi Circuit Breaker mở hoặc timeout
     * Không cho phép tạo đơn khi chưa xác thực được user
     */
    public CompletableFuture<UserValidationResponse> validateUserAsyncFallback(
            Long userId, String jwtToken, Exception ex) {
        log.error("Circuit Breaker fallback triggered for user validation. Error: {}", ex.getMessage());
        log.warn("User Service unavailable - rejecting order creation");
        return CompletableFuture.failedFuture(
                new OrderValidationException("Không thể xác thực user, vui lòng thử lại sau")
        );
    }
}

