package com.example.order_service.application.usecase;

import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.repository.OrderRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Use case to validate if a restaurant can be deleted.
 * Checks if there are any active orders for the restaurant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidateRestaurantDeleteUseCase {

    private final OrderRepository orderRepository;

    public ValidationResult execute(Long merchantId) {
        log.info("Validating restaurant deletion for merchantId: {}", merchantId);

        // Active statuses that prevent deletion
        List<OrderStatus> activeStatuses = Arrays.asList(
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED,
                OrderStatus.PAID,
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERING);

        // Count active orders and collect status details
        long totalActiveOrders = 0;
        List<String> activeStatusNames = new ArrayList<>();

        for (OrderStatus status : activeStatuses) {
            int count = orderRepository.findByMerchantIdAndStatus(merchantId, status).size();
            if (count > 0) {
                totalActiveOrders += count;
                activeStatusNames.add(status.name() + "(" + count + ")");
            }
        }

        boolean canDelete = totalActiveOrders == 0;
        String reason = canDelete
                ? "No active orders found"
                : "Cannot delete restaurant with active orders";

        log.info("Validation result for merchantId {}: canDelete={}, activeOrders={}, statuses={}",
                merchantId, canDelete, totalActiveOrders, activeStatusNames);

        return ValidationResult.builder()
                .canDelete(canDelete)
                .activeOrderCount(totalActiveOrders)
                .activeStatuses(activeStatusNames)
                .reason(reason)
                .build();
    }

    @Builder
    @Data
    public static class ValidationResult {
        private boolean canDelete;
        private long activeOrderCount;
        private List<String> activeStatuses;
        private String reason;
    }
}
