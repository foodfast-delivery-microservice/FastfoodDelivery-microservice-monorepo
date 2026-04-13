package com.example.paymentservice.infrastructure.persistence.adapter;

import com.example.paymentservice.domain.entities.Payment;
import com.example.paymentservice.domain.repository.PaymentRepository;
import com.example.paymentservice.domain.valueobjects.PageRequest;
import com.example.paymentservice.domain.valueobjects.PageResult;
import com.example.paymentservice.infrastructure.persistence.entity.PaymentJpaEntity;
import com.example.paymentservice.infrastructure.persistence.mapper.PaymentMapper;
import com.example.paymentservice.infrastructure.persistence.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing domain PaymentRepository using JPA infrastructure.
 * Bridges the gap between domain layer and infrastructure layer.
 */
@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity jpaEntity = PaymentMapper.toJpaEntity(payment);
        PaymentJpaEntity saved = paymentJpaRepository.save(jpaEntity);
        return PaymentMapper.toDomainEntity(saved);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentJpaRepository.findById(id)
                .map(PaymentMapper::toDomainEntity);
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentJpaRepository.findByOrderId(orderId)
                .map(PaymentMapper::toDomainEntity);
    }

    @Override
    public List<Payment> findByUserId(Long userId) {
        return paymentJpaRepository.findByUserId(userId).stream()
                .map(PaymentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<Payment> findByMerchantId(Long merchantId, PageRequest pageRequest) {
        Pageable pageable = PageRequestConverter.toSpringPageable(pageRequest);
        Page<PaymentJpaEntity> jpaPage = paymentJpaRepository.findByMerchantId(merchantId, pageable);
        List<Payment> domainEntities = jpaPage.getContent().stream()
                .map(PaymentMapper::toDomainEntity)
                .collect(Collectors.toList());
        Page<Payment> domainPage = new PageImpl<>(domainEntities, pageable, jpaPage.getTotalElements());
        return PageResultConverter.toDomainPageResult(domainPage, pageRequest);
    }

    @Override
    public PageResult<Payment> findByMerchantIdAndStatus(Long merchantId, Payment.Status status, PageRequest pageRequest) {
        Pageable pageable = PageRequestConverter.toSpringPageable(pageRequest);
        Page<PaymentJpaEntity> jpaPage = paymentJpaRepository.findByMerchantIdAndStatus(merchantId, status, pageable);
        List<Payment> domainEntities = jpaPage.getContent().stream()
                .map(PaymentMapper::toDomainEntity)
                .collect(Collectors.toList());
        Page<Payment> domainPage = new PageImpl<>(domainEntities, pageable, jpaPage.getTotalElements());
        return PageResultConverter.toDomainPageResult(domainPage, pageRequest);
    }

    @Override
    public List<Payment> findByMerchantIdAndStatus(Long merchantId, Payment.Status status) {
        return paymentJpaRepository.findByMerchantIdAndStatus(merchantId, status).stream()
                .map(PaymentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<Payment> findByMerchantIdAndCreatedAtBetween(
                    Long merchantId,
                    LocalDateTime fromDate,
                    LocalDateTime toDate,
                    PageRequest pageRequest) {
        Pageable pageable = PageRequestConverter.toSpringPageable(pageRequest);
        Page<PaymentJpaEntity> jpaPage = paymentJpaRepository.findByMerchantIdAndCreatedAtBetween(
                merchantId, fromDate, toDate, pageable);
        List<Payment> domainEntities = jpaPage.getContent().stream()
                .map(PaymentMapper::toDomainEntity)
                .collect(Collectors.toList());
        Page<Payment> domainPage = new PageImpl<>(domainEntities, pageable, jpaPage.getTotalElements());
        return PageResultConverter.toDomainPageResult(domainPage, pageRequest);
    }

    @Override
    public PageResult<Payment> findByMerchantIdAndStatusAndCreatedAtBetween(
                    Long merchantId,
                    Payment.Status status,
                    LocalDateTime fromDate,
                    LocalDateTime toDate,
                    PageRequest pageRequest) {
        Pageable pageable = PageRequestConverter.toSpringPageable(pageRequest);
        Page<PaymentJpaEntity> jpaPage = paymentJpaRepository.findByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, status, fromDate, toDate, pageable);
        List<Payment> domainEntities = jpaPage.getContent().stream()
                .map(PaymentMapper::toDomainEntity)
                .collect(Collectors.toList());
        Page<Payment> domainPage = new PageImpl<>(domainEntities, pageable, jpaPage.getTotalElements());
        return PageResultConverter.toDomainPageResult(domainPage, pageRequest);
    }

    @Override
    public BigDecimal sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                    Long merchantId,
                    Payment.Status status,
                    LocalDateTime fromDate,
                    LocalDateTime toDate) {
        return paymentJpaRepository.sumAmountByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, status, fromDate, toDate);
    }

    @Override
    public Long countByMerchantIdAndStatusAndCreatedAtBetween(
                    Long merchantId,
                    Payment.Status status,
                    LocalDateTime fromDate,
                    LocalDateTime toDate) {
        return paymentJpaRepository.countByMerchantIdAndStatusAndCreatedAtBetween(
                merchantId, status, fromDate, toDate);
    }

    @Override
    public BigDecimal sumAmountByMerchantIdAndStatus(Long merchantId, Payment.Status status) {
        return paymentJpaRepository.sumAmountByMerchantIdAndStatus(merchantId, status);
    }

    @Override
    public Long countByMerchantIdAndStatus(Long merchantId, Payment.Status status) {
        return paymentJpaRepository.countByMerchantIdAndStatus(merchantId, status);
    }
}
