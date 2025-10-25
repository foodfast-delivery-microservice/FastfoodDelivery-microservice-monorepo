package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.OrderListRequest;
import com.example.order_service.application.dto.OrderListResponse;
import com.example.order_service.application.dto.PageResponse;
import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetOrderListUseCase {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public PageResponse<OrderListResponse> execute(OrderListRequest request) {
        log.info("Getting order list with request: {}", request);

        // Build specification for filtering
        Specification<Order> spec = buildSpecification(request);

        // Build pageable for pagination and sorting
        Pageable pageable = buildPageable(request);

        // Query orders
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        // Convert to response
        List<OrderListResponse> orderResponses = orderPage.getContent().stream()
                .map(this::mapToOrderListResponse)
                .collect(Collectors.toList());

        return PageResponse.<OrderListResponse>builder()
                .content(orderResponses)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .hasNext(orderPage.hasNext())
                .hasPrevious(orderPage.hasPrevious())
                .build();
    }

    private Specification<Order> buildSpecification(OrderListRequest request) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Filter by userId
            if (request.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), request.getUserId()));
            }

            // Filter by status
            if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                try {
                    OrderStatus status = OrderStatus.valueOf(request.getStatus().toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid order status: {}", request.getStatus());
                }
            }

            // Filter by order code
            if (request.getOrderCode() != null && !request.getOrderCode().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("orderCode")),
                        "%" + request.getOrderCode().toLowerCase() + "%"
                ));
            }

            // Filter by date range
            if (request.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), request.getFromDate()));
            }

            if (request.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), request.getToDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Pageable buildPageable(OrderListRequest request) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, request.getSortBy());

        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    private OrderListResponse mapToOrderListResponse(Order order) {
        return OrderListResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .currency(order.getCurrency())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getGrandTotal())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .receiverName(order.getDeliveryAddress().getReceiverName())
                .receiverPhone(order.getDeliveryAddress().getReceiverPhone())
                .fullAddress(order.getDeliveryAddress().getFullAddress())
                .itemCount(order.getOrderItems().size())
                .build();
    }
}
