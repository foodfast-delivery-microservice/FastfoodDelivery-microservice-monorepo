package com.example.droneservice.infrastructure.persistence.adapter;

import com.example.droneservice.domain.entities.DroneMission;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import com.example.droneservice.domain.valueobjects.Status;
import com.example.droneservice.infrastructure.persistence.entity.DroneMissionJpaEntity;
import com.example.droneservice.infrastructure.persistence.mapper.DroneMapper;
import com.example.droneservice.infrastructure.persistence.mapper.DroneMissionMapper;
import com.example.droneservice.infrastructure.persistence.repository.DroneMissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Infrastructure implementation of DroneMissionRepository.
 * Adapts between domain and JPA using DroneMissionMapper.
 */
@Repository
@RequiredArgsConstructor
public class DroneMissionRepositoryImpl implements DroneMissionRepository {

    private final DroneMissionJpaRepository jpaRepository;
    private final DroneMissionMapper mapper;
    private final DroneMapper droneMapper;

    @Override
    public DroneMission save(DroneMission mission) {
        DroneMissionJpaEntity entity = mapper.toEntity(mission);
        DroneMissionJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved, droneMapper);
    }

    @Override
    public Optional<DroneMission> findById(Long id) {
        return jpaRepository.findById(id)
                .map(entity -> mapper.toDomain(entity, droneMapper));
    }

    @Override
    public List<DroneMission> findByDroneId(Long droneId) {
        return jpaRepository.findByDroneId(droneId)
                .stream()
                .map(entity -> mapper.toDomain(entity, droneMapper))
                .collect(Collectors.toList());
    }

    @Override
    public List<DroneMission> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId)
                .stream()
                .map(entity -> mapper.toDomain(entity, droneMapper))
                .collect(Collectors.toList());
    }

    @Override
    public List<DroneMission> findByStatus(Status status) {
        return jpaRepository.findByStatus(status.name())
                .stream()
                .map(entity -> mapper.toDomain(entity, droneMapper))
                .collect(Collectors.toList());
    }

    @Override
    public List<DroneMission> findByStatusIn(List<Status> statuses) {
        List<String> statusNames = statuses.stream()
                .map(Status::name)
                .collect(Collectors.toList());

        return jpaRepository.findByStatusIn(statusNames)
                .stream()
                .map(entity -> mapper.toDomain(entity, droneMapper))
                .collect(Collectors.toList());
    }

    @Override
    public List<DroneMission> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(entity -> mapper.toDomain(entity, droneMapper))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(DroneMission mission) {
        if (mission.getId() != null) {
            jpaRepository.deleteById(mission.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
