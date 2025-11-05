package com.example.order_service.infrastructure.service;

import com.example.order_service.application.dto.ProductValidationRequest;
import com.example.order_service.application.dto.ProductValidationResponse;
import com.example.order_service.application.dto.ProductValidationWrapper;
import com.example.order_service.domain.repository.ProductServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceAdapter implements ProductServicePort {

    private final WebClient productWebClient;

    @Override
    public List<ProductValidationResponse> validateProducts(List<ProductValidationRequest> request) {
        try {
            log.info("=== CALLING PRODUCT SERVICE ===");
            log.info("Endpoint: /validate");
            log.info("Request body: {}", request);

            JwtAuthenticationToken authentication = (JwtAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();
            String jwtToken = authentication.getToken().getTokenValue();

            // Gọi Product Service và nhận wrapper response
            ProductValidationWrapper wrapper = productWebClient.post()
                    .uri("/validate")
                    .header("Authorization", "Bearer " + jwtToken)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ProductValidationWrapper.class)
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
        } catch (Exception e) {
            log.error("Error calling Product Service", e);
            throw new RuntimeException("Cannot connect to Product Service", e);
        }
    }
}