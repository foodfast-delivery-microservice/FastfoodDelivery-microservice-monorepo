package com.example.order_service.infrastructure.persistence.mapper;

import com.example.order_service.domain.entities.OrderItem;
import com.example.order_service.domain.valueobjects.Money;
import com.example.order_service.infrastructure.persistence.entity.OrderItemJpaEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper to convert between OrderItem domain entity and OrderItemJpaEntity.
 */
public class OrderItemMapper {

    /**
     * Convert domain entity to JPA entity
     * Note: Does NOT set the order reference - this should be done by the caller
     */
    public static OrderItemJpaEntity toJpaEntity(OrderItem domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        OrderItemJpaEntity jpaEntity = new OrderItemJpaEntity();
        jpaEntity.setId(domainEntity.getId());
        jpaEntity.setProductId(domainEntity.getProductId());
        jpaEntity.setMerchantId(domainEntity.getMerchantId());
        jpaEntity.setProductName(domainEntity.getProductName());

        // Convert Money to BigDecimal
        if (domainEntity.getUnitPrice() != null) {
            jpaEntity.setUnitPrice(domainEntity.getUnitPrice().getAmount());
            jpaEntity.setCurrency(domainEntity.getUnitPrice().getCurrency());
        }

        jpaEntity.setQuantity(domainEntity.getQuantity());

        if (domainEntity.getLineTotal() != null) {
            jpaEntity.setLineTotal(domainEntity.getLineTotal().getAmount());
        }

        return jpaEntity;
    }

    /**
     * Convert JPA entity to domain entity
     */
    public static OrderItem toDomainEntity(OrderItemJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        String currency = jpaEntity.getCurrency() != null ? jpaEntity.getCurrency() : "VND";

        OrderItem domainEntity = OrderItem.builder()
                .id(jpaEntity.getId())
                .productId(jpaEntity.getProductId())
                .merchantId(jpaEntity.getMerchantId())
                .productName(jpaEntity.getProductName())
                .unitPrice(jpaEntity.getUnitPrice() != null ? new Money(jpaEntity.getUnitPrice(), currency) : null)
                .quantity(jpaEntity.getQuantity())
                .lineTotal(jpaEntity.getLineTotal() != null ? new Money(jpaEntity.getLineTotal(), currency) : null)
                .build();

        return domainEntity;
    }

    /**
     * Convert list of domain entities to JPA entities
     */
    public static List<OrderItemJpaEntity> toJpaEntities(List<OrderItem> domainEntities) {
        if (domainEntities == null) {
            return null;
        }

        return domainEntities.stream()
                .map(OrderItemMapper::toJpaEntity)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of JPA entities to domain entities
     */
    public static List<OrderItem> toDomainEntities(List<OrderItemJpaEntity> jpaEntities) {
        if (jpaEntities == null) {
            return null;
        }

        return jpaEntities.stream()
                .map(OrderItemMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}
