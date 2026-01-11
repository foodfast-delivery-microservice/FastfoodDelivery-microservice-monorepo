package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.OrderDetailResponse;
import com.example.order_service.domain.exception.OrderNotFoundException;
import com.example.order_service.domain.entities.Order;
import com.example.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetOrderDetailUseCase {

        private final OrderRepository orderRepository;

        @Transactional(readOnly = true)
        public OrderDetailResponse execute(Long orderId) {
                log.info("Getting order detail for orderId: {}", orderId);

                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

                return OrderDetailResponse.fromEntity(order);
        }
}
