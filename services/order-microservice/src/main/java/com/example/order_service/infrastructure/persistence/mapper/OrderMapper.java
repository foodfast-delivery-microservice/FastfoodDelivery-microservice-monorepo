package com.example.order_service.infrastructure.persistence.mapper;

import com.example.order_service.domain.entities.Order;
import com.example.order_service.domain.entities.OrderItem;
import com.example.order_service.domain.valueobjects.Money;
import com.example.order_service.domain.valueobjects.OrderCode;
import com.example.order_service.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.example.order_service.infrastructure.persistence.entity.OrderJpaEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper to convert between Order domain entity and OrderJpaEntity.
 */
public class OrderMapper {

    /**
     * Convert domain entity to JPA entity
     */
    public static OrderJpaEntity toJpaEntity(Order domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        OrderJpaEntity jpaEntity = new OrderJpaEntity();
        jpaEntity.setId(domainEntity.getId());
        jpaEntity.setOrderCode(domainEntity.getOrderCode() != null ? domainEntity.getOrderCode().getValue() : null);
        jpaEntity.setUserId(domainEntity.getUserId());
        jpaEntity.setMerchantId(domainEntity.getMerchantId());
        jpaEntity.setStatus(domainEntity.getStatus());

        // Convert Money value objects to BigDecimal
        String currency = domainEntity.getSubtotal() != null ? domainEntity.getSubtotal().getCurrency() : "VND";
        jpaEntity.setCurrency(currency);

        jpaEntity.setSubtotal(domainEntity.getSubtotal() != null ? domainEntity.getSubtotal().getAmount() : null);
        jpaEntity.setDiscount(domainEntity.getDiscount() != null ? domainEntity.getDiscount().getAmount()
                : Money.zeroVND().getAmount());
        jpaEntity.setShippingFee(domainEntity.getShippingFee() != null ? domainEntity.getShippingFee().getAmount()
                : Money.zeroVND().getAmount());
        jpaEntity.setGrandTotal(domainEntity.getGrandTotal() != null ? domainEntity.getGrandTotal().getAmount() : null);

        jpaEntity.setNote(domainEntity.getNote());

        // Convert DeliveryAddress value object
        jpaEntity.setDeliveryAddress(
                DeliveryAddressMapper.toEmbeddable(domainEntity.getDeliveryAddress()));

        jpaEntity.setCreatedAt(domainEntity.getCreatedAt());
        jpaEntity.setProcessingStartedAt(domainEntity.getProcessingStartedAt());

        // Convert OrderItems
        if (domainEntity.getOrderItems() != null) {
            List<OrderItemJpaEntity> jpaItems = new ArrayList<>();
            for (OrderItem item : domainEntity.getOrderItems()) {
                OrderItemJpaEntity jpaItem = OrderItemMapper.toJpaEntity(item);
                jpaEntity.addOrderItem(jpaItem); // Sets bidirectional relationship
            }
        }

        return jpaEntity;
    }

    /**
     * Convert JPA entity to domain entity
     */
    public static Order toDomainEntity(OrderJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        String currency = jpaEntity.getCurrency() != null ? jpaEntity.getCurrency() : "VND";

        Order domainEntity = Order.builder()
                .id(jpaEntity.getId())
                .orderCode(jpaEntity.getOrderCode() != null ? new OrderCode(jpaEntity.getOrderCode()) : null)
                .userId(jpaEntity.getUserId())
                .merchantId(jpaEntity.getMerchantId())
                .status(jpaEntity.getStatus())
                .subtotal(jpaEntity.getSubtotal() != null ? new Money(jpaEntity.getSubtotal(), currency) : null)
                .discount(jpaEntity.getDiscount() != null ? new Money(jpaEntity.getDiscount(), currency)
                        : Money.zero(currency))
                .shippingFee(jpaEntity.getShippingFee() != null ? new Money(jpaEntity.getShippingFee(), currency)
                        : Money.zero(currency))
                .grandTotal(jpaEntity.getGrandTotal() != null ? new Money(jpaEntity.getGrandTotal(), currency) : null)
                .note(jpaEntity.getNote())
                .deliveryAddress(
                        DeliveryAddressMapper.toValueObject(jpaEntity.getDeliveryAddress()))
                .createdAt(jpaEntity.getCreatedAt())
                .processingStartedAt(jpaEntity.getProcessingStartedAt())
                .orderItems(OrderItemMapper.toDomainEntities(jpaEntity.getOrderItems()))
                .build();

        return domainEntity;
    }
}
