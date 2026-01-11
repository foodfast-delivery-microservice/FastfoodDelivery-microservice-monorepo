package com.example.order_service.infrastructure.persistence.repository;

import com.example.order_service.domain.valueobjects.OrderStatus;
import com.example.order_service.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for OrderJpaEntity persistence.
 * This is the infrastructure layer repository with Spring Data JPA.
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long>,
        JpaSpecificationExecutor<OrderJpaEntity> {

    Optional<OrderJpaEntity> findByOrderCode(String orderCode);

    List<OrderJpaEntity> findByUserId(Long userId);

    Page<OrderJpaEntity> findByUserId(Long userId, Pageable pageable);

    List<OrderJpaEntity> findByStatus(OrderStatus status);

    List<OrderJpaEntity> findByUserIdAndStatus(Long userId, OrderStatus status);

    List<OrderJpaEntity> findByMerchantIdAndStatus(Long merchantId, OrderStatus status);

    @Query("SELECT o FROM OrderJpaEntity o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<OrderJpaEntity> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM OrderJpaEntity o WHERE o.userId = :userId AND o.createdAt BETWEEN :startDate AND :endDate")
    List<OrderJpaEntity> findByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM OrderJpaEntity o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<OrderJpaEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    boolean existsByOrderCode(String orderCode);

    @Query("SELECT COUNT(o) FROM OrderJpaEntity o WHERE o.userId = :userId AND o.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);

    long countByUserId(Long userId);

    @Query("SELECT SUM(o.grandTotal) FROM OrderJpaEntity o WHERE o.userId = :userId AND o.status IN :statuses")
    BigDecimal sumGrandTotalByUserIdAndStatusIn(
            @Param("userId") Long userId,
            @Param("statuses") Collection<OrderStatus> statuses);

    // Admin analytics queries
    long countByStatus(OrderStatus status);

    long countByCreatedAtAfter(LocalDateTime dateTime);
}
