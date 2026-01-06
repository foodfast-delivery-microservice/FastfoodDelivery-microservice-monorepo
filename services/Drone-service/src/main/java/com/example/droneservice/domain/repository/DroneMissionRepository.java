package com.example.droneservice.domain.repository;

import com.example.droneservice.domain.entities.DroneMission;
import com.example.droneservice.domain.valueobjects.Status;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for DroneMission.
 * This is a pure domain interface with no framework dependencies.
 * Implementations are in the infrastructure layer.
 */
public interface DroneMissionRepository {

    /**
     * Save a drone mission (create or update)
     */
    DroneMission save(DroneMission mission);

    /**
     * Find mission by ID
     */
    Optional<DroneMission> findById(Long id);

    /**
     * Find all missions for a specific drone
     */
    List<DroneMission> findByDroneId(Long droneId);

    /**
     * Find all missions for a specific order
     */
    List<DroneMission> findByOrderId(Long orderId);

    /**
     * Find all missions with a specific status
     */
    List<DroneMission> findByStatus(Status status);

    /**
     * Find all missions with specific statuses (e.g., ASSIGNED, IN_PROGRESS)
     */
    List<DroneMission> findByStatusIn(List<Status> statuses);

    /**
     * Find all missions
     */
    List<DroneMission> findAll();

    /**
     * Delete a mission
     */
    void delete(DroneMission mission);

    /**
     * Delete mission by ID
     */
    void deleteById(Long id);
}
