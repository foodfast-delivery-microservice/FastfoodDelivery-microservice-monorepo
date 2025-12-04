package com.example.demo.interfaces.rest.dto.restaurant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateRestaurantRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String address;

    private String city;

    private String district;

    private String description;

    private String image;

    private String phone;

    private String email;

    private String openingHours;

    private String category;

    private BigDecimal deliveryFee;

    private Integer estimatedDeliveryTime;

    @NotNull
    private Boolean active;
}












