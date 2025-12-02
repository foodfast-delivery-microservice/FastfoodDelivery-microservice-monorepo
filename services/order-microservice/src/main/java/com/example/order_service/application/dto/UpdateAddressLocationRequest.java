package com.example.order_service.application.dto;

import com.example.order_service.domain.model.AddressSource;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressLocationRequest {

    @NotNull(message = "Latitude không được để trống")
    private BigDecimal lat;

    @NotNull(message = "Longitude không được để trống")
    private BigDecimal lng;

    private AddressSource source;
}



