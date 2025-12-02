package com.example.order_service.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    private Long userId;

    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<OrderItemRequest> orderItems;

    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    private String note;

    private Long deliveryAddressId;

    @Valid
    private DeliveryAddressRequest deliveryAddress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {

        @NotNull(message = "Mã sản phẩm là bắt buộc")
        private Long productId;

        @NotNull(message = "Số lượng là bắt buộc")
        @Positive(message = "Số lượng phải lớn hơn 0")
        private Integer quantity;
        
        // Note: productName and unitPrice will be fetched from Product Service
        // based on productId
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAddressRequest {

        @NotBlank(message = "Tên người nhận không được để trống")
        @Size(min = 2, max = 100, message = "Tên người nhận phải có từ 2 đến 100 ký tự")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "Tên người nhận chỉ được chứa chữ cái, số và khoảng trắng")
        private String receiverName;

        @NotBlank(message = "Số điện thoại không được để trống")
        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại không hợp lệ. Phải có 10 số, bắt đầu bằng 0")
        private String receiverPhone;

        @Size(min = 3, max = 255, message = "Địa chỉ chi tiết phải có từ 3 đến 255 ký tự")
        private String addressLine1;

        @Size(min = 2, max = 100, message = "Phường/Xã phải có từ 2 đến 100 ký tự")
        private String ward;

        @Size(min = 2, max = 100, message = "Quận/Huyện phải có từ 2 đến 100 ký tự")
        private String district;

        @Size(min = 2, max = 100, message = "Thành phố/Tỉnh phải có từ 2 đến 100 ký tự")
        private String city;

        // Optional: Latitude and Longitude for GPS coordinates
        private BigDecimal lat;
        private BigDecimal lng;

        // Optional: normalized administrative fields from AddressKit/NSO.
        // These are not required from clients; backend can enrich them.
        private String provinceCode;
        private String provinceName;
        private String communeCode;
        private String communeName;
        private String normalizedDistrictName;
    }
}
