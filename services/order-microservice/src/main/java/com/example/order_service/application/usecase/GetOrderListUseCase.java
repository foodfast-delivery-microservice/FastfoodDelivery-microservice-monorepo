package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.OrderListRequest;
import com.example.order_service.application.dto.OrderListResponse;
import com.example.order_service.application.dto.PageResponse;
import com.example.order_service.domain.valueobjects.OrderQuery;
import com.example.order_service.domain.valueobjects.OrderStatus;
import com.example.order_service.domain.valueobjects.PageRequest;
import com.example.order_service.domain.valueobjects.PageResult;
import com.example.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        // Build domain query object
        OrderQuery query = buildOrderQuery(request);

        // Query orders using domain repository
        PageResult<com.example.order_service.domain.entities.Order> orderPage = orderRepository.findAll(query);

        // Convert to response
        List<OrderListResponse> orderResponses = orderPage.getContent().stream()
                .map(OrderListResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<OrderListResponse>builder()
                .content(orderResponses)
                .page(orderPage.getPage())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .hasNext(orderPage.isHasNext())
                .hasPrevious(orderPage.isHasPrevious())
                .build();
    }

    private OrderQuery buildOrderQuery(OrderListRequest request) {
        OrderStatus status = null;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                status = OrderStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid order status: {}", request.getStatus());
            }
        }

        PageRequest.SortDirection sortDirection = "ASC".equalsIgnoreCase(request.getSortDirection())
                ? PageRequest.SortDirection.ASC
                : PageRequest.SortDirection.DESC;

        PageRequest pageRequest = PageRequest.builder()
                .page(request.getPage())
                .size(request.getSize())
                .sortBy(request.getSortBy())
                .sortDirection(sortDirection)
                .build();

        return OrderQuery.builder()
                .userId(request.getUserId())
                .merchantId(request.getMerchantId())
                .status(status)
                .orderCode(request.getOrderCode())
                .startDate(request.getFromDate())
                .endDate(request.getToDate())
                .pageRequest(pageRequest)
                .build();
    }
}
