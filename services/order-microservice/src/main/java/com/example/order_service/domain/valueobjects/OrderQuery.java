package com.example.order_service.domain.valueobjects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain value object for order query criteria.
 * This replaces JPA Specification in the domain layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQuery {
    private Long userId;
    private Long merchantId;
    private OrderStatus status;
    private List<OrderStatus> statuses;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String orderCode;
    private PageRequest pageRequest;
}
