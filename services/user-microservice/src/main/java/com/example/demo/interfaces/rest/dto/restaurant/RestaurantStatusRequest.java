package com.example.demo.interfaces.rest.dto.restaurant;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RestaurantStatusRequest {
    @NotNull
    private Boolean active;
}

















