package com.example.droneservice.application.dto;

import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.State;
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
        DroneResponse droneResponse = new DroneResponse();
        droneResponse.setId(drone.getId());
        droneResponse.setSerialNumber(drone.getSerialNumber());
        droneResponse.setModel(drone.getModel());
        droneResponse.setBatteryLevel(drone.getBatteryLevel());
        droneResponse.setState(drone.getState());
        droneResponse.setCurrentLatitude(drone.getCurrentLatitude());
        droneResponse.setCurrentLongitude(drone.getCurrentLongitude());
        droneResponse.setBaseLatitude(drone.getBaseLatitude());
        droneResponse.setBaseLongitude(drone.getBaseLongitude());
        droneResponse.setWeightCapacity(drone.getWeightCapacity());
        return droneResponse;
    }
}
