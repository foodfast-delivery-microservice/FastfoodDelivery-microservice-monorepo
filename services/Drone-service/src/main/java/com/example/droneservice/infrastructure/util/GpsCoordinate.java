package com.example.droneservice.infrastructure.util;

import com.example.droneservice.domain.valueobjects.Coordinates;
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
    private Coordinates coordinates;
}
