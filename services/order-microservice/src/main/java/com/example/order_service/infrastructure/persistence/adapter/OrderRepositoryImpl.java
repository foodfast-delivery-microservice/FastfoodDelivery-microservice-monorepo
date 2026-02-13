package com.example.order_service.infrastructure.persistence.adapter;

import com.example.order_service.domain.entities.Order;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.domain.valueobjects.OrderQuery;
import com.example.order_service.domain.valueobjects.OrderStatus;
import com.example.order_service.domain.valueobjects.PageResult;
import com.example.order_service.infrastructure.persistence.entity.OrderJpaEntity;
import com.example.order_service.infrastructure.persistence.mapper.OrderMapper;
import com.example.order_service.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing domain OrderRepository using JPA infrastructure.
 * Bridges the gap between domain layer and infrastructure layer.
 */
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        OrderJpaEntity jpaEntity = OrderMapper.toJpaEntity(order);
        OrderJpaEntity savedEntity = orderJpaRepository.save(jpaEntity);
        return OrderMapper.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id)
                .map(OrderMapper::toDomainEntity);
    }

    @Override
    public List<Order> findAll() {
        return orderJpaRepository.findAll().stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        orderJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return orderJpaRepository.existsById(id);
    }

    @Override
    public PageResult<Order> findAll(OrderQuery query) {
        // Build JPA Specification from domain query
        Specification<OrderJpaEntity> spec = buildSpecification(query);
        
        // Convert domain PageRequest to Spring Pageable
        Pageable pageable = PageRequestConverter.toSpringPageable(query.getPageRequest());
        
        // Query using JPA repository
        Page<OrderJpaEntity> jpaPage = orderJpaRepository.findAll(spec, pageable);
        
        // Convert to domain entities
        List<Order> domainOrders = jpaPage.getContent().stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
        
        // Convert Spring Page to domain PageResult
        return PageResultConverter.toDomainPageResult(
                new org.springframework.data.domain.PageImpl<>(domainOrders, pageable, jpaPage.getTotalElements()),
                query.getPageRequest()
        );
    }
    
    private Specification<OrderJpaEntity> buildSpecification(OrderQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            
            if (query.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), query.getUserId()));
            }
            
            if (query.getMerchantId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("merchantId"), query.getMerchantId()));
            }
            
            if (query.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), query.getStatus()));
            }
            
            if (query.getStatuses() != null && !query.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(query.getStatuses()));
            }
            
            if (query.getOrderCode() != null && !query.getOrderCode().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("orderCode")),
                        "%" + query.getOrderCode().toLowerCase() + "%"));
            }
            
            if (query.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), query.getStartDate()));
            }
            
            if (query.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), query.getEndDate()));
            }
            
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Override
    public Optional<Order> findByOrderCode(String orderCode) {
        return orderJpaRepository.findByOrderCode(orderCode)
                .map(OrderMapper::toDomainEntity);
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return orderJpaRepository.findByUserId(userId).stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return orderJpaRepository.findByStatus(status).stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByUserIdAndStatus(Long userId, OrderStatus status) {
        return orderJpaRepository.findByUserIdAndStatus(userId, status).stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByMerchantIdAndStatus(Long merchantId, OrderStatus status) {
        return orderJpaRepository.findByMerchantIdAndStatus(merchantId, status).stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return orderJpaRepository.findByCreatedAtBetween(startDate, endDate).stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByUserIdAndCreatedAtBetween(
            Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return orderJpaRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate).stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return orderJpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByOrderCode(String orderCode) {
        return orderJpaRepository.existsByOrderCode(orderCode);
    }

    @Override
    public long countByUserIdAndStatus(Long userId, OrderStatus status) {
        return orderJpaRepository.countByUserIdAndStatus(userId, status);
    }

    @Override
    public long countByUserId(Long userId) {
        return orderJpaRepository.countByUserId(userId);
    }

    @Override
    public BigDecimal sumGrandTotalByUserIdAndStatusIn(
            Long userId, Collection<OrderStatus> statuses) {
        return orderJpaRepository.sumGrandTotalByUserIdAndStatusIn(userId, statuses);
    }

    @Override
    public long countByStatus(OrderStatus status) {
        return orderJpaRepository.countByStatus(status);
    }

    @Override
    public long countByCreatedAtAfter(LocalDateTime dateTime) {
        return orderJpaRepository.countByCreatedAtAfter(dateTime);
    }
}
