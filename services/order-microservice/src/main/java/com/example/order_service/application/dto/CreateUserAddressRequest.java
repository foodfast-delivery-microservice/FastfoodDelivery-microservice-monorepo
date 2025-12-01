package com.example.order_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserAddressRequest {

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    @Size(min = 3, max = 255, message = "Địa chỉ chi tiết phải từ 3 đến 255 ký tự")
    private String street;

    @NotBlank(message = "Mã tỉnh/thành không được để trống")
    private String provinceCode;

    @NotBlank(message = "Mã xã/phường không được để trống")
    private String communeCode;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    private String note;
}



