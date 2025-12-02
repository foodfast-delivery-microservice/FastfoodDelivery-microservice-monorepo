package com.example.droneservice.domain.repository;

import com.example.droneservice.domain.model.DroneMission;
import com.example.droneservice.domain.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DroneMissionRepository extends JpaRepository<DroneMission, Long> {

    List<DroneMission> findByStatus(Status status);

    List<DroneMission> findByStatusIn(List<Status> statuses);

    Optional<DroneMission> findByOrderId(Long orderId);

    List<DroneMission> findByDroneId(Long droneId);
}
