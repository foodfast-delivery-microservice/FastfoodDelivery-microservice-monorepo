package com.example.droneservice.domain.repository;

import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.valueobjects.State;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for Drone.
 * This is a pure domain interface with no framework dependencies.
 * Implementations are in the infrastructure layer.
 */
public interface DroneRepository {

    /**
     * Save a drone (create or update)
     */
    Drone save(Drone drone);

    /**
     * Find drone by ID
     */
    Optional<Drone> findById(Long id);

    /**
     * Find drone by serial number
     */
    Optional<Drone> findBySerialNumber(String serialNumber);

    /**
     * Find all drones with a specific state
     */
    List<Drone> findByState(State state);

    /**
     * Find all drones with any of the specified states
     */
    List<Drone> findByStates(List<State> states);

    /**
     * Find all drones
     */
    List<Drone> findAll();

    /**
     * Save multiple drone entities
     */
    List<Drone> saveAll(List<Drone> drones);

    /**
     * Delete a drone
     */
    void delete(Drone drone);

    /**
     * Delete drone by ID
     */
    void deleteById(Long id);

    List<Drone> findAllByStateIn(List<State> states);
}
