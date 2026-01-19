package com.example.paymentservice.infrastructure.persistence.mapper;

import com.example.paymentservice.domain.entities.Payment;
import com.example.paymentservice.infrastructure.persistence.entity.PaymentJpaEntity;

/**
 * Mapper to convert between Payment domain entity and PaymentJpaEntity.
 */
public class PaymentMapper {

    /**
     * Convert domain entity to JPA entity
     */
    public static PaymentJpaEntity toJpaEntity(Payment domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        PaymentJpaEntity jpaEntity = new PaymentJpaEntity();
        jpaEntity.setId(domainEntity.getId());
        jpaEntity.setOrderId(domainEntity.getOrderId());
        jpaEntity.setUserId(domainEntity.getUserId());
        jpaEntity.setMerchantId(domainEntity.getMerchantId());
        jpaEntity.setAmount(domainEntity.getAmount());
        jpaEntity.setCurrency(domainEntity.getCurrency());
        jpaEntity.setStatus(domainEntity.getStatus());
        jpaEntity.setCreatedAt(domainEntity.getCreatedAt());
        jpaEntity.setUpdatedAt(domainEntity.getUpdatedAt());
        jpaEntity.setTransactionNo(domainEntity.getTransactionNo());
        jpaEntity.setFailReason(domainEntity.getFailReason());
        jpaEntity.setRefundAmount(domainEntity.getRefundAmount());

        return jpaEntity;
    }

    /**
     * Convert JPA entity to domain entity
     */
    public static Payment toDomainEntity(PaymentJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        Payment domainEntity = Payment.builder()
                .id(jpaEntity.getId())
                .orderId(jpaEntity.getOrderId())
                .userId(jpaEntity.getUserId())
                .merchantId(jpaEntity.getMerchantId())
                .amount(jpaEntity.getAmount())
                .currency(jpaEntity.getCurrency())
                .status(jpaEntity.getStatus())
                .createdAt(jpaEntity.getCreatedAt())
                .updatedAt(jpaEntity.getUpdatedAt())
                .transactionNo(jpaEntity.getTransactionNo())
                .failReason(jpaEntity.getFailReason())
                .refundAmount(jpaEntity.getRefundAmount())
                .build();

        return domainEntity;
    }
}
