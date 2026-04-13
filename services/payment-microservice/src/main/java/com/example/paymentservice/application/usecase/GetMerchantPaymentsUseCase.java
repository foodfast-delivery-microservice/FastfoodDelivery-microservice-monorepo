package com.example.paymentservice.application.usecase;

import com.example.paymentservice.application.dto.PageResponse;
import com.example.paymentservice.application.dto.PaymentListRequest;
import com.example.paymentservice.application.dto.PaymentResponse;
import com.example.paymentservice.domain.entities.Payment;
import com.example.paymentservice.domain.repository.PaymentRepository;
import com.example.paymentservice.domain.valueobjects.PageRequest;
import com.example.paymentservice.domain.valueobjects.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        // Build domain page request
        PageRequest pageRequest = buildPageRequest(request);

        // Query payments based on filters using domain repository
        PageResult<Payment> paymentPage;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                Payment.Status status = Payment.Status.valueOf(request.getStatus().toUpperCase());
                if (request.getFromDate() != null && request.getToDate() != null) {
                    paymentPage = paymentRepository.findByMerchantIdAndStatusAndCreatedAtBetween(
                            merchantId, status, request.getFromDate(), request.getToDate(), pageRequest);
                } else {
                    paymentPage = paymentRepository.findByMerchantIdAndStatus(merchantId, status, pageRequest);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid payment status: {}", request.getStatus());
                // Fallback to query without status filter
                if (request.getFromDate() != null && request.getToDate() != null) {
                    paymentPage = paymentRepository.findByMerchantIdAndCreatedAtBetween(
                            merchantId, request.getFromDate(), request.getToDate(), pageRequest);
                } else {
                    paymentPage = paymentRepository.findByMerchantId(merchantId, pageRequest);
                }
            }
        } else if (request.getFromDate() != null && request.getToDate() != null) {
            paymentPage = paymentRepository.findByMerchantIdAndCreatedAtBetween(
                    merchantId, request.getFromDate(), request.getToDate(), pageRequest);
        } else {
            paymentPage = paymentRepository.findByMerchantId(merchantId, pageRequest);
        }

        // Convert to response
        List<PaymentResponse> paymentResponses = paymentPage.getContent().stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());

        return PageResponse.<PaymentResponse>builder()
                .content(paymentResponses)
                .page(paymentPage.getPage())
                .size(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .first(paymentPage.isFirst())
                .last(paymentPage.isLast())
                .hasNext(paymentPage.isHasNext())
                .hasPrevious(paymentPage.isHasPrevious())
                .build();
    }

    private PageRequest buildPageRequest(PaymentListRequest request) {
        PageRequest.SortDirection sortDirection = "ASC".equalsIgnoreCase(request.getSortDirection())
                ? PageRequest.SortDirection.ASC
                : PageRequest.SortDirection.DESC;

        return PageRequest.builder()
                .page(request.getPage())
                .size(request.getSize())
                .sortBy(request.getSortBy())
                .sortDirection(sortDirection)
                .build();
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

