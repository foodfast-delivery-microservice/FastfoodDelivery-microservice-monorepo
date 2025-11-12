package com.example.payment.application.usecase;

import com.example.payment.application.dto.PageResponse;
import com.example.payment.application.dto.PaymentListRequest;
import com.example.payment.application.dto.PaymentResponse;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetMerchantPaymentsUseCase {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> execute(Long merchantId, PaymentListRequest request) {
        log.info("Getting payments for merchant: {} with request: {}", merchantId, request);

        // Build pageable for pagination and sorting
        Pageable pageable = buildPageable(request);

        // Query payments based on filters
        Page<Payment> paymentPage;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                Payment.Status status = Payment.Status.valueOf(request.getStatus().toUpperCase());
                if (request.getFromDate() != null && request.getToDate() != null) {
                    paymentPage = paymentRepository.findByMerchantIdAndStatusAndCreatedAtBetween(
                            merchantId, status, request.getFromDate(), request.getToDate(), pageable);
                } else {
                    paymentPage = paymentRepository.findByMerchantIdAndStatus(merchantId, status, pageable);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid payment status: {}", request.getStatus());
                // Fallback to query without status filter
                if (request.getFromDate() != null && request.getToDate() != null) {
                    paymentPage = paymentRepository.findByMerchantIdAndCreatedAtBetween(
                            merchantId, request.getFromDate(), request.getToDate(), pageable);
                } else {
                    paymentPage = paymentRepository.findByMerchantId(merchantId, pageable);
                }
            }
        } else if (request.getFromDate() != null && request.getToDate() != null) {
            paymentPage = paymentRepository.findByMerchantIdAndCreatedAtBetween(
                    merchantId, request.getFromDate(), request.getToDate(), pageable);
        } else {
            paymentPage = paymentRepository.findByMerchantId(merchantId, pageable);
        }

        // Convert to response
        List<PaymentResponse> paymentResponses = paymentPage.getContent().stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());

        return PageResponse.<PaymentResponse>builder()
                .content(paymentResponses)
                .page(paymentPage.getNumber())
                .size(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .first(paymentPage.isFirst())
                .last(paymentPage.isLast())
                .hasNext(paymentPage.hasNext())
                .hasPrevious(paymentPage.hasPrevious())
                .build();
    }

    private Pageable buildPageable(PaymentListRequest request) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, request.getSortBy());

        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .merchantId(payment.getMerchantId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .transactionNo(payment.getTransactionNo())
                .failReason(payment.getFailReason())
                .timestamp(payment.getCreatedAt())
                .build();
    }
}

