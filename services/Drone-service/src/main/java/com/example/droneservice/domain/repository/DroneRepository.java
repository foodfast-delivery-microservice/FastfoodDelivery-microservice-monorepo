package com.example.droneservice.domain.repository;

import com.example.droneservice.domain.model.Drone;
import com.example.droneservice.domain.model.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DroneRepository extends JpaRepository<Drone, Long> {

    List<Drone> findByState(State state);

    Optional<Drone> findBySerialNumber(String serialNumber);

    List<Drone> findAllByStateIn(List<State>  states);
}

