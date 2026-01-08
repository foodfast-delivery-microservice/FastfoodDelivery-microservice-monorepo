package com.example.droneservice.infrastructure.persistence.repository;

import com.example.droneservice.domain.valueobjects.State;
import com.example.droneservice.infrastructure.persistence.entity.DroneJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for DroneJpaEntity.
 * This interface is part of the infrastructure layer.
 */
public interface DroneJpaRepository extends JpaRepository<DroneJpaEntity, Long> {

    Optional<DroneJpaEntity> findBySerialNumber(String serialNumber);

    List<DroneJpaEntity> findByState(State state);

    List<DroneJpaEntity> findByStateIn(List<String> states);
}
