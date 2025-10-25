package com.example.order_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {

    private Long id;
    private String orderCode;
    private Long userId;
    private String status;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private BigDecimal grandTotal;
    private String note;
    private LocalDateTime createdAt;
    private DeliveryAddressResponse deliveryAddress;
    private List<OrderItemResponse> orderItems;
}
