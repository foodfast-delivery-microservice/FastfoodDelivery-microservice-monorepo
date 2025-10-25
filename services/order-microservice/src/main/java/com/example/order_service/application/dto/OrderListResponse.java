package com.example.order_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResponse {

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
    private String receiverName;
    private String receiverPhone;
    private String fullAddress;
    private int itemCount;
}
