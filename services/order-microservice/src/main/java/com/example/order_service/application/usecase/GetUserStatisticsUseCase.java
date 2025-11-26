package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.UserStatisticsResponse;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetUserStatisticsUseCase {

    private final OrderRepository orderRepository;

    public UserStatisticsResponse execute(Long userId) {
        log.info("Calculating statistics for user: {}", userId);

        long totalOrders = orderRepository.countByUserId(userId);
        BigDecimal totalSpent = orderRepository.sumGrandTotalByUserIdAndStatusIn(userId,
                java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.DELIVERED));

        if (totalSpent == null) {
            totalSpent = BigDecimal.ZERO;
        }

        return UserStatisticsResponse.builder()
                .userId(userId)
                .totalOrders(totalOrders)
                .totalSpent(totalSpent)
                .build();
    }
}
