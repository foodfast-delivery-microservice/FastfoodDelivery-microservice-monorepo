package com.example.droneservice.application.usecases.mission;

import com.example.droneservice.application.DTOs.mission.MissionResponse;
import com.example.droneservice.domain.entities.DroneMission;
import com.example.droneservice.domain.exception.MissionNotFoundException;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for getting mission by order ID
 */
@Service
@RequiredArgsConstructor
public class GetMissionByOrderIdUseCase {
    private final DroneMissionRepository missionRepository;

    public MissionResponse execute(Long orderId) {
        List<DroneMission> missions = missionRepository.findByOrderId(orderId);

        if (missions.isEmpty()) {
            throw new MissionNotFoundException(orderId);
        }

        // Get first mission (should only be one mission per order)
        return MissionResponse.fromEntity(missions.get(0));
    }
}
