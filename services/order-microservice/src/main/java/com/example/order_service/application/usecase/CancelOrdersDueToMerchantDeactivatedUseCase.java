package com.example.order_service.application.usecase;

import com.example.order_service.domain.model.EventStatus;
import com.example.order_service.domain.model.Order;
import com.example.order_service.domain.model.OrderStatus;
import com.example.order_service.domain.model.OutboxEvent;
import com.example.order_service.domain.repository.OrderRepository;
import com.example.order_service.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CancelOrdersDueToMerchantDeactivatedUseCase {

    private static final String CANCEL_REASON = "Merchant deactivated";

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void execute(Long merchantId) {
        log.info("Cancelling orders for merchant {} due to deactivation", merchantId);
        List<OrderStatus> targetStatuses = Arrays.asList(OrderStatus.PENDING, OrderStatus.CONFIRMED);
        targetStatuses.forEach(status -> cancelOrdersByStatus(merchantId, status));
    }

    private void cancelOrdersByStatus(Long merchantId, OrderStatus status) {
        List<Order> orders = orderRepository.findByMerchantIdAndStatus(merchantId, status);
        if (orders.isEmpty()) {
            log.debug("No orders with status {} for merchant {}", status, merchantId);
            return;
        }
        orders.forEach(this::cancelOrder);
    }

    private void cancelOrder(Order order) {
        OrderStatus previousStatus = order.getStatus();
        try {
            order.cancel();
            order.setNote(CANCEL_REASON);
            orderRepository.save(order);
            createStatusChangeEvent(order, previousStatus, OrderStatus.CANCELLED, CANCEL_REASON);
            log.info("Order {} cancelled due to merchant deactivation (previous status: {})", order.getId(), previousStatus);
        } catch (Exception e) {
            log.error("Failed to cancel order {} while processing merchant deactivation", order.getId(), e);
        }
    }

    private void createStatusChangeEvent(Order order, OrderStatus oldStatus, OrderStatus newStatus, String note) {
        try {
            ObjectNode eventData = objectMapper.createObjectNode();
            eventData.put("orderId", order.getId());
            eventData.put("orderCode", order.getOrderCode());
            eventData.put("userId", order.getUserId());
            eventData.put("oldStatus", oldStatus.name());
            eventData.put("newStatus", newStatus.name());
            eventData.put("note", note);
            eventData.put("timestamp", LocalDateTime.now().toString());

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .type("OrderStatusChanged")
                    .payload(objectMapper.writeValueAsString(eventData))
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to create outbox event for order {}", order.getId(), e);
        }
    }
}

