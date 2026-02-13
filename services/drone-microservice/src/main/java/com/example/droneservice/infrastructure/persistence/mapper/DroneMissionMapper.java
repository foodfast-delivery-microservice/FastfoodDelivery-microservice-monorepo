package com.example.droneservice.infrastructure.persistence.mapper;

import com.example.droneservice.domain.entities.DroneMission;
import com.example.droneservice.domain.valueobjects.Coordinates;
import com.example.droneservice.domain.valueobjects.Status;
import com.example.droneservice.infrastructure.persistence.entity.DroneMissionJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper to convert between DroneMission domain entity and
 * DroneMissionJpaEntity.
 * This is part of the infrastructure layer and depends on domain types.
 */
@Component
public class DroneMissionMapper {

    /**
     * Convert JPA entity to domain entity (without drone to avoid circular
     * reference)
     */
    public DroneMission toDomainWithoutDrone(DroneMissionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        DroneMission mission = new DroneMission();
        mission.setId(entity.getId());
        mission.setDrone(null); // Avoid circular reference
        mission.setOrderId(entity.getOrderId());

        mission.setPickupLocation(new Coordinates(
                entity.getPickupLatitude(),
                entity.getPickupLongitude()));

        mission.setDeliveryLocation(new Coordinates(
                entity.getDeliveryLatitude(),
                entity.getDeliveryLongitude()));

        mission.setStatus(entity.getStatus());
        mission.setDistanceKm(entity.getDistanceKm());
        mission.setEstimatedDurationMinutes(entity.getEstimatedDurationMinutes());
        mission.setStartedAt(entity.getStartedAt());
        mission.setCompletedAt(entity.getCompletedAt());

        return mission;
    }

    /**
     * Convert domain entity to JPA entity (without drone reference)
     */
    public DroneMissionJpaEntity toEntity(DroneMission domain) {
        if (domain == null) {
            return null;
        }

        DroneMissionJpaEntity entity = new DroneMissionJpaEntity();
        entity.setId(domain.getId());
        // Drone reference handled separately by repository
        entity.setOrderId(domain.getOrderId());

        if (domain.getPickupLocation() != null) {
            entity.setPickupLatitude(domain.getPickupLocation().getLatitude());
            entity.setPickupLongitude(domain.getPickupLocation().getLongitude());
        }

        if (domain.getDeliveryLocation() != null) {
            entity.setDeliveryLatitude(domain.getDeliveryLocation().getLatitude());
            entity.setDeliveryLongitude(domain.getDeliveryLocation().getLongitude());
        }

        entity.setStatus(domain.getStatus());
        entity.setDistanceKm(domain.getDistanceKm());
        entity.setEstimatedDurationMinutes(domain.getEstimatedDurationMinutes());
        entity.setStartedAt(domain.getStartedAt());
        entity.setCompletedAt(domain.getCompletedAt());

        return entity;
    }

    /**
     * Convert JPA entity to domain entity with drone reference
     * Use DroneMapper separately to set the drone
     */
    public DroneMission toDomain(DroneMissionJpaEntity entity, DroneMapper droneMapper) {
        if (entity == null) {
            return null;
        }

        DroneMission mission = toDomainWithoutDrone(entity);

        if (entity.getDrone() != null) {
            mission.setDrone(droneMapper.toDomainWithoutMissions(entity.getDrone()));
        }

        return mission;
    }
}
