package com.example.order_service.domain.repository;

import com.example.order_service.domain.entities.Order;
import com.example.order_service.domain.valueobjects.OrderQuery;
import com.example.order_service.domain.valueobjects.OrderStatus;
import com.example.order_service.domain.valueobjects.PageRequest;
import com.example.order_service.domain.valueobjects.PageResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Pure domain repository interface for Order aggregate.
 * All methods use domain value objects, no framework dependencies.
 */
public interface OrderRepository {

        // Basic CRUD
        Order save(Order order);

        Optional<Order> findById(Long id);

        List<Order> findAll();

        void deleteById(Long id);

        boolean existsById(Long id);

        // Dynamic queries using domain query object
        PageResult<Order> findAll(OrderQuery query);

        // Business queries
        Optional<Order> findByOrderCode(String orderCode);

        List<Order> findByUserId(Long userId);

        List<Order> findByStatus(OrderStatus status);

        List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

        List<Order> findByMerchantIdAndStatus(Long merchantId, OrderStatus status);

        List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

        List<Order> findByUserIdAndCreatedAtBetween(
                        Long userId,
                        LocalDateTime startDate,
                        LocalDateTime endDate);

        List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

        boolean existsByOrderCode(String orderCode);

        // Analytics queries
        long countByUserIdAndStatus(Long userId, OrderStatus status);

        long countByUserId(Long userId);

        BigDecimal sumGrandTotalByUserIdAndStatusIn(
                        Long userId,
                        Collection<OrderStatus> statuses);

        long countByStatus(OrderStatus status);

        long countByCreatedAtAfter(LocalDateTime dateTime);
}
