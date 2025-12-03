package com.example.droneservice.infrastructure.service;

import com.example.droneservice.infrastructure.client.dto.OrderDetailResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
public class OrderServiceAdapter {

    private final WebClient orderWebClient;
    private final ObjectMapper objectMapper;

    public OrderServiceAdapter(WebClient orderWebClient, ObjectMapper objectMapper) {
        this.orderWebClient = orderWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Get order detail from Order Service
     * @param orderId Order ID
     * @return OrderDetailResponse
     */
    public OrderDetailResponse getOrderDetail(Long orderId) {
        try {
            log.info("Calling Order Service to get order detail: orderId={}", orderId);

            String jwtToken = getJwtToken();

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

            log.info("Order Service response received for orderId: {}", orderId);

            // Parse response
            OrderDetailResponse orderDetail = objectMapper.readValue(responseJson, OrderDetailResponse.class);

            log.info("Order detail retrieved: orderId={}, status={}", 
                    orderDetail.getId(), orderDetail.getStatus());

            return orderDetail;

        } catch (WebClientResponseException.NotFound ex) {
            log.warn("Order {} not found (404)", orderId);
            throw new RuntimeException("Order not found with id: " + orderId);
        } catch (WebClientResponseException.Forbidden ex) {
            log.error("Access forbidden when calling order service (403). OrderId: {}", orderId);
            throw new RuntimeException("Không có quyền truy cập Order Service. Vui lòng kiểm tra token và quyền truy cập.");
        } catch (WebClientResponseException.Unauthorized ex) {
            log.error("Auth failed when calling order service (401)", ex);
            throw new RuntimeException("Token không hợp lệ hoặc đã hết hạn");
        } catch (WebClientResponseException e) {
            log.error("HTTP error when calling Order Service: {} - {}", e.getStatusCode(), e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối đến Order Service: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            log.error("WebClient exception when calling Order Service (network/timeout)", e);
            throw new RuntimeException("Không thể kết nối đến Order Service: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error calling Order Service", e);
            throw new RuntimeException("Lỗi không mong đợi khi gọi Order Service: " + e.getMessage());
        }
    }

    /**
     * Get JWT token from SecurityContext
     */
    private String getJwtToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            log.warn("Authentication is null or not a JwtAuthenticationToken - trying without token");
            return null; // Return null, let the service handle it
        }
        
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        return jwtAuth.getToken().getTokenValue();
    }
}

