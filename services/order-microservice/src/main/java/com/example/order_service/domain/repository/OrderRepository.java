package com.example.order_service.domain.repository;

import com.example.order_service.domain.entities.Order;
import com.example.order_service.domain.valueobjects.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Pure domain repository interface for Order aggregate.
 * Note: Contains some JPA-specific methods (Specification, Pageable) for
 * pragmatic reasons.
 * TODO: Replace with pure domain query objects in future refactoring.
 */
public interface OrderRepository {

        // Basic CRUD
        Order save(Order order);

        Optional<Order> findById(Long id);

        List<Order> findAll();

        void deleteById(Long id);

        boolean existsById(Long id);

        // Dynamic queries (JPA-specific - pragmatic approach)
        Page<Order> findAll(Specification<Order> spec, Pageable pageable);

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
