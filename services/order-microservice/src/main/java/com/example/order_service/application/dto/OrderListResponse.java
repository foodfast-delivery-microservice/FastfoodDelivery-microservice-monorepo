package com.example.order_service.application.dto;

import com.example.order_service.domain.entities.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResponse {

    private Long id;
    private String orderCode;
    private Long userId;
    private Long merchantId;
    private String status;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private BigDecimal grandTotal;
    private String note;
    private LocalDateTime createdAt;
    private String receiverName;
    private String receiverPhone;
    private String fullAddress;
    private int itemCount;
    private List<OrderItemResponse> orderItems;

    /**
     * Static factory method to create OrderListResponse from domain entity
     */
    public static OrderListResponse fromEntity(Order order) {
        if (order == null) {
            return null;
        }

        return OrderListResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode() != null ? order.getOrderCode().getValue() : null)
                .userId(order.getUserId())
                .merchantId(order.getMerchantId())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .currency(order.getSubtotal() != null ? order.getSubtotal().getCurrency() : "VND")
                .subtotal(order.getSubtotal() != null ? order.getSubtotal().getAmount() : null)
                .discount(order.getDiscount() != null ? order.getDiscount().getAmount() : BigDecimal.ZERO)
                .shippingFee(order.getShippingFee() != null ? order.getShippingFee().getAmount() : BigDecimal.ZERO)
                .grandTotal(order.getGrandTotal() != null ? order.getGrandTotal().getAmount() : null)
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .receiverName(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getReceiverName() : null)
                .receiverPhone(
                        order.getDeliveryAddress() != null ? order.getDeliveryAddress().getReceiverPhone() : null)
                .fullAddress(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getFullAddress() : null)
                .itemCount(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                .orderItems(order.getOrderItems() != null ? order.getOrderItems().stream()
                        .map(OrderItemResponse::fromEntity)
                        .collect(Collectors.toList())
                        : null)
                .build();
    }
}
