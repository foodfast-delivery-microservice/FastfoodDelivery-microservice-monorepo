package com.example.droneservice.infrastructure.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple GPS coordinate holder
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GpsCoordinate {
    private Double latitude;
    private Double longitude;
}
