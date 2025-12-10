package com.example.order_service.application.usecase;

import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckMerchantOrdersUseCase {

    private final OrderRepository orderRepository;

    /**
     * Kiểm tra xem merchant có thể bị xóa không.
     * Merchant chỉ có thể xóa khi TẤT CẢ orders đều ở trạng thái DELIVERED hoặc
     * CANCELLED.
     */
    public MerchantOrderCheckResult execute(Long merchantId) {
        log.info("Checking if merchant {} can be deleted", merchantId);

        // Các trạng thái "active" - không cho phép xóa merchant nếu có orders ở các
        // trạng thái này
        List<OrderStatus> activeStatuses = Arrays.asList(
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED,
                OrderStatus.PAID,
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERING);

        // Đếm tổng số orders active và thu thập thông tin chi tiết
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

        log.info("Merchant {} deletion check: canDelete={}, activeOrders={}, statuses={}",
                merchantId, canDelete, totalActiveOrders, activeStatusNames);

        return MerchantOrderCheckResult.builder()
                .canDelete(canDelete)
                .activeOrderCount(totalActiveOrders)
                .activeStatuses(activeStatusNames)
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class MerchantOrderCheckResult {
        private boolean canDelete;
        private long activeOrderCount;
        private List<String> activeStatuses;
    }
}
