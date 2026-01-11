package com.example.order_service.application.dto;

import com.example.order_service.domain.entities.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private Long merchantId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;

    /**
     * Static factory method to create response from domain entity
     */
    public static OrderItemResponse fromEntity(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .merchantId(orderItem.getMerchantId())
                .productName(orderItem.getProductName())
                .unitPrice(orderItem.getUnitPrice() != null ? orderItem.getUnitPrice().getAmount() : null)
                .quantity(orderItem.getQuantity())
                .lineTotal(orderItem.getLineTotal() != null ? orderItem.getLineTotal().getAmount() : null)
                .build();
    }
}
