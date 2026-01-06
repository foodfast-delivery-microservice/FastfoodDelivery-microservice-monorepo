package com.example.droneservice.application.DTOs.drone;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating drone battery level
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBatteryRequest {

    @NotNull(message = "Battery level is required")
    @Min(value = 0, message = "Battery level must be at least 0")
    @Max(value = 100, message = "Battery level must be at most 100")
    private Integer batteryLevel;
}
