package com.example.droneservice.infrastructure.persistence.adapter;

import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.repository.DroneRepository;
import com.example.droneservice.domain.valueobjects.State;
import com.example.droneservice.infrastructure.persistence.entity.DroneJpaEntity;
import com.example.droneservice.infrastructure.persistence.mapper.DroneMapper;
import com.example.droneservice.infrastructure.persistence.repository.DroneJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Infrastructure implementation of DroneRepository.
 * Adapts between domain and JPA using DroneMapper.
 */
@Repository
@RequiredArgsConstructor
public class DroneRepositoryImpl implements DroneRepository {

    private final DroneJpaRepository jpaRepository;
    private final DroneMapper mapper;

    @Override
    public Drone save(Drone drone) {
        DroneJpaEntity jpaEntity = mapper.toEntity(drone);
        DroneJpaEntity saved = jpaRepository.save(jpaEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Drone> saveAll(List<Drone> drones) {
        // Convert domain entities to JPA entities
        List<DroneJpaEntity> jpaEntities = drones.stream()
                .map(mapper::toEntity)
                .collect(java.util.stream.Collectors.toList());

        // Save all and convert back to domain
        return jpaRepository.saveAll(jpaEntities).stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Optional<Drone> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Drone> findBySerialNumber(String serialNumber) {
        return jpaRepository.findBySerialNumber(serialNumber)
                .map(mapper::toDomain);
    }

    @Override
    public List<Drone> findByState(State state) {
        return jpaRepository.findByState(state.name())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Drone> findByStates(List<State> states) {
        List<String> stateNames = states.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        return jpaRepository.findByStateIn(stateNames)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Drone> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Drone drone) {
        if (drone.getId() != null) {
            jpaRepository.deleteById(drone.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Drone> findAllByStateIn(List<State> states) {
        // Convert domain State enum names to Strings for JPA repository
        List<String> stateNames = states.stream()
                .map(State::name)
                .collect(java.util.stream.Collectors.toList());

        // Find by state names and convert to domain entities
        return jpaRepository.findByStateIn(stateNames).stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

}
