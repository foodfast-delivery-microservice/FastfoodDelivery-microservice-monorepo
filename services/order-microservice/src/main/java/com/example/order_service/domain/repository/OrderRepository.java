package com.example.order_service.domain.repository;

import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

        Optional<Order> findByOrderCode(String orderCode);

        List<Order> findByUserId(Long userId);

        Page<Order> findByUserId(Long userId, Pageable pageable);

        List<Order> findByStatus(OrderStatus status);

        List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

        List<Order> findByMerchantIdAndStatus(Long merchantId, OrderStatus status);

        @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
        List<Order> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.createdAt BETWEEN :startDate AND :endDate")
        List<Order> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
        List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

        boolean existsByOrderCode(String orderCode);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status = :status")
        long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);

        long countByUserId(Long userId);

        @Query("SELECT SUM(o.grandTotal) FROM Order o WHERE o.userId = :userId AND o.status IN :statuses")
        java.math.BigDecimal sumGrandTotalByUserIdAndStatusIn(@Param("userId") Long userId,
                        @Param("statuses") java.util.Collection<OrderStatus> statuses);

        // Admin analytics queries
        long countByStatus(OrderStatus status);

        long countByCreatedAtAfter(LocalDateTime dateTime);

        // Revenue by merchant for delivered orders
        @Query(value = "SELECT merchant_id, SUM(grand_total) as revenue " +
               "FROM orders " +
               "WHERE status = :status " +
               "AND merchant_id IS NOT NULL " +
               "GROUP BY merchant_id " +
               "ORDER BY revenue DESC", nativeQuery = true)
        List<Object[]> sumRevenueByMerchantIdAndStatus(@Param("status") String status);

        // Revenue by merchant for delivered orders with date range
        @Query(value = "SELECT merchant_id, SUM(grand_total) as revenue " +
               "FROM orders " +
               "WHERE status = :status " +
               "AND merchant_id IS NOT NULL " +
               "AND created_at BETWEEN :fromDate AND :toDate " +
               "GROUP BY merchant_id " +
               "ORDER BY revenue DESC", nativeQuery = true)
        List<Object[]> sumRevenueByMerchantIdAndStatusAndDateRange(
                @Param("status") String status,
                @Param("fromDate") LocalDateTime fromDate,
                @Param("toDate") LocalDateTime toDate);

        /**
         * Override mặc định của JpaSpecificationExecutor để luôn fetch join collection orderItems
         * nhằm tránh N+1 query khi load danh sách đơn hàng (đặc biệt khi dùng DB từ xa như Railway).
         *
         * Với @EntityGraph, Spring Data sẽ sinh truy vấn dạng LEFT JOIN FETCH o.orderItems
         * nhưng vẫn hỗ trợ phân trang.
         */
        @Override
        @EntityGraph(attributePaths = "orderItems")
        Page<Order> findAll(@Nullable Specification<Order> spec, Pageable pageable);
}
