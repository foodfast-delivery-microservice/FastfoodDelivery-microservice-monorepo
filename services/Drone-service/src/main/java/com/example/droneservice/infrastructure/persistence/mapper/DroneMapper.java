package com.example.droneservice.infrastructure.persistence.mapper;

import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.valueobjects.*;
import com.example.droneservice.infrastructure.persistence.entity.DroneJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Mapper to convert between Drone domain entity and DroneJpaEntity.
 * This is part of the infrastructure layer and depends on domain types.
 */
@Component
public class DroneMapper {

    private final DroneMissionMapper missionMapper;

    public DroneMapper(DroneMissionMapper missionMapper) {
        this.missionMapper = missionMapper;
    }

    /**
     * Convert JPA entity to domain entity
     */
    public Drone toDomain(DroneJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Drone drone = new Drone();
        drone.setId(entity.getId());
        drone.setSerialNumber(new SerialNumber(entity.getSerialNumber()));
        drone.setModel(entity.getModel());
        drone.setBatteryLevel(new BatteryLevel(entity.getBatteryLevel()));
        drone.setState(State.valueOf(entity.getState()));

        if (entity.getCurrentLatitude() != null && entity.getCurrentLongitude() != null) {
            drone.setCurrentLocation(new Coordinates(
                    entity.getCurrentLatitude(),
                    entity.getCurrentLongitude()));
        }

        if (entity.getBaseLatitude() != null && entity.getBaseLongitude() != null) {
            drone.setBaseLocation(new Coordinates(
                    entity.getBaseLatitude(),
                    entity.getBaseLongitude()));
        }

        if (entity.getWeightCapacity() != null) {
            drone.setWeightCapacity(new WeightCapacity(entity.getWeightCapacity()));
        }

        // Convert missions (avoid circular dependency by not mapping drone back)
        if (entity.getMissions() != null) {
            drone.setMissions(
                    entity.getMissions().stream()
                            .map(missionMapper::toDomainWithoutDrone)
                            .collect(Collectors.toList()));
        } else {
            drone.setMissions(new ArrayList<>());
        }

        return drone;
    }

    /**
     * Convert domain entity to JPA entity
     */
    public DroneJpaEntity toEntity(Drone domain) {
        if (domain == null) {
            return null;
        }

        DroneJpaEntity entity = new DroneJpaEntity();
        entity.setId(domain.getId());
        entity.setSerialNumber(domain.getSerialNumber().getValue());
        entity.setModel(domain.getModel());
        entity.setBatteryLevel(domain.getBatteryLevel().getValue());
        entity.setState(domain.getState().name());

        if (domain.getCurrentLocation() != null) {
            entity.setCurrentLatitude(domain.getCurrentLocation().getLatitude());
            entity.setCurrentLongitude(domain.getCurrentLocation().getLongitude());
        }

        if (domain.getBaseLocation() != null) {
            entity.setBaseLatitude(domain.getBaseLocation().getLatitude());
            entity.setBaseLongitude(domain.getBaseLocation().getLongitude());
        }

        if (domain.getWeightCapacity() != null) {
            entity.setWeightCapacity(domain.getWeightCapacity().getValue());
        }

        // Missions are handled separately to avoid circular dependency
        if (entity.getMissions() == null) {
            entity.setMissions(new ArrayList<>());
        }

        return entity;
    }

    /**
     * Convert domain to JPA without nested missions (for updates)
     */
    public DroneJpaEntity toEntityWithoutMissions(Drone domain) {
        return toEntity(domain);
    }

    /**
     * Convert JPA to domain without missions (to avoid circular references)
     */
    public Drone toDomainWithoutMissions(DroneJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Drone drone = new Drone();
        drone.setId(entity.getId());
        drone.setSerialNumber(new SerialNumber(entity.getSerialNumber()));
        drone.setModel(entity.getModel());
        drone.setBatteryLevel(new BatteryLevel(entity.getBatteryLevel()));
        drone.setState(State.valueOf(entity.getState()));

        if (entity.getCurrentLatitude() != null && entity.getCurrentLongitude() != null) {
            drone.setCurrentLocation(new Coordinates(
                    entity.getCurrentLatitude(),
                    entity.getCurrentLongitude()));
        }

        if (entity.getBaseLatitude() != null && entity.getBaseLongitude() != null) {
            drone.setBaseLocation(new Coordinates(
                    entity.getBaseLatitude(),
                    entity.getBaseLongitude()));
        }

        if (entity.getWeightCapacity() != null) {
            drone.setWeightCapacity(new WeightCapacity(entity.getWeightCapacity()));
        }

        drone.setMissions(new ArrayList<>());

        return drone;
    }
}
