package com.example.droneservice.application.DTOs.drone;

import com.example.droneservice.domain.valueobjects.State;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating drone state
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStateRequest {

    @NotNull(message = "State is required")
    private State state;
}
