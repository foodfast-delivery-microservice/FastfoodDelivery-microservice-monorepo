package com.example.demo.infrastructure.client;

import com.example.demo.infrastructure.client.dto.MerchantOrderCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client to communicate with Order Service
 * Uses service discovery via Eureka to find order-service
 */
@FeignClient(name = "order-service", path = "/api/internal/orders")
public interface OrderServiceClient {

    /**
     * Check if merchant can be deleted (all orders must be DELIVERED or CANCELLED)
     * 
     * @param merchantId ID of the merchant to check
     * @return MerchantOrderCheckResponse with canDelete flag and active order
     *         details
     */
    @GetMapping("/merchant/{merchantId}/can-delete")
    MerchantOrderCheckResponse checkMerchantCanBeDeleted(@PathVariable("merchantId") Long merchantId);
}
