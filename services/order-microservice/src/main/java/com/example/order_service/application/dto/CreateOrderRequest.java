package com.example.order_service.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemRequest> orderItems;

    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    private String note;

    @NotNull(message = "Delivery address is required")
    @Valid
    private DeliveryAddressRequest deliveryAddress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {

        @NotNull(message = "Product ID is required")
        private String productId;

        @NotNull(message = "Product name is required")
        private String productName;

        @NotNull(message = "Unit price is required")
        @Positive(message = "Unit price must be positive")
        private BigDecimal unitPrice;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAddressRequest {

        @NotNull(message = "Receiver name is required")
        private String receiverName;

        @NotNull(message = "Receiver phone is required")
        private String receiverPhone;

        @NotNull(message = "Address line 1 is required")
        private String addressLine1;

        @NotNull(message = "Ward is required")
        private String ward;

        @NotNull(message = "District is required")
        private String district;

        @NotNull(message = "City is required")
        private String city;
    }
}
