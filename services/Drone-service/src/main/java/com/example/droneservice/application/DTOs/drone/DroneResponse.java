package com.example.droneservice.application.dto;

import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.valueobjects.State;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DroneResponse {
    private Long id;
    private String serialNumber;
    private String model;
    private Integer batteryLevel;
    private State state;
    private Double currentLatitude;
    private Double currentLongitude;
    private Double baseLatitude;
    private Double baseLongitude;
    private Double weightCapacity;

    public static DroneResponse fromEntity(Drone drone) {
        return DroneResponse.builder()
                .id(drone.getId())
                .serialNumber(drone.getSerialNumber() != null ? drone.getSerialNumber().getValue() : null)
                .model(drone.getModel())
                .batteryLevel(drone.getBatteryLevel() != null ? drone.getBatteryLevel().getValue() : null)
                .state(drone.getState())
                .currentLatitude(drone.getCurrentLocation() != null ? drone.getCurrentLocation().getLatitude() : null)
                .currentLongitude(drone.getCurrentLocation() != null ? drone.getCurrentLocation().getLongitude() : null)
                .baseLatitude(drone.getBaseLocation() != null ? drone.getBaseLocation().getLatitude() : null)
                .baseLongitude(drone.getBaseLocation() != null ? drone.getBaseLocation().getLongitude() : null)
                .weightCapacity(drone.getWeightCapacity() != null ? drone.getWeightCapacity().getValue() : null)
                .build();
    }
}
