package com.example.order_service.application.dto;

import com.example.order_service.domain.entities.Order;
import com.example.order_service.domain.entities.OrderItem;
import com.example.order_service.domain.valueobjects.DeliveryAddress;
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
public class OrderResponse {

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
    private LocalDateTime processingStartedAt;
    private DeliveryAddressResponse deliveryAddress;
    private List<OrderItemResponse> orderItems;

    /**
     * Static factory method to create OrderResponse from domain entity
     */
    public static OrderResponse fromEntity(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponse.builder()
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
                .processingStartedAt(order.getProcessingStartedAt())
                .deliveryAddress(DeliveryAddressResponse.fromEntity(order.getDeliveryAddress()))
                .orderItems(order.getOrderItems() != null ? order.getOrderItems().stream()
                        .map(OrderItemResponse::fromEntity)
                        .collect(Collectors.toList())
                        : null)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAddressResponse {
        private String receiverName;
        private String receiverPhone;
        private String addressLine1;
        private String ward;
        private String district;
        private String city;
        private BigDecimal lat;
        private BigDecimal lng;
        private String provinceCode;
        private String provinceName;
        private String communeCode;
        private String communeName;
        private String normalizedDistrictName;
        private String fullAddress;

        /**
         * Static factory method for nested DeliveryAddressResponse
         */
        public static DeliveryAddressResponse fromEntity(DeliveryAddress deliveryAddress) {
            if (deliveryAddress == null) {
                return null;
            }

            return DeliveryAddressResponse.builder()
                    .receiverName(deliveryAddress.getReceiverName())
                    .receiverPhone(deliveryAddress.getReceiverPhone())
                    .addressLine1(deliveryAddress.getAddressLine1())
                    .ward(deliveryAddress.getWard())
                    .district(deliveryAddress.getDistrict())
                    .city(deliveryAddress.getCity())
                    .lat(deliveryAddress.getLat())
                    .lng(deliveryAddress.getLng())
                    .provinceCode(deliveryAddress.getProvinceCode())
                    .provinceName(deliveryAddress.getProvinceName())
                    .communeCode(deliveryAddress.getCommuneCode())
                    .communeName(deliveryAddress.getCommuneName())
                    .normalizedDistrictName(deliveryAddress.getNormalizedDistrictName())
                    .fullAddress(deliveryAddress.getFullAddress())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private Long merchantId;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal lineTotal;

        /**
         * Static factory method for nested OrderItemResponse
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
}
