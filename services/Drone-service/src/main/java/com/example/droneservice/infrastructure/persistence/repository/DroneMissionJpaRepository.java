package com.example.droneservice.infrastructure.persistence.repository;

import com.example.droneservice.infrastructure.persistence.entity.DroneMissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for DroneMissionJpaEntity.
 * This interface is part of the infrastructure layer.
 */
public interface DroneMissionJpaRepository extends JpaRepository<DroneMissionJpaEntity, Long> {

    List<DroneMissionJpaEntity> findByDroneId(Long droneId);

    List<DroneMissionJpaEntity> findByOrderId(Long orderId);

    List<DroneMissionJpaEntity> findByStatus(String status);

    List<DroneMissionJpaEntity> findByStatusIn(List<String> statuses);
}
