package com.example.droneservice.application.usecases.mission;

import com.example.droneservice.application.DTOs.mission.TrackingResponse;
import com.example.droneservice.domain.entities.DroneMission;
import com.example.droneservice.domain.exception.MissionNotFoundException;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for getting real-time mission tracking information
 */
@Service
@RequiredArgsConstructor
public class GetMissionTrackingUseCase {
    private final DroneMissionRepository missionRepository;

    /**
     * Get tracking info by mission ID
     */
    public TrackingResponse execute(Long missionId) {
        DroneMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionNotFoundException(missionId));

        return TrackingResponse.fromEntity(mission);
    }

    /**
     * Get tracking info by order ID (convenience method for frontend)
     */
    public TrackingResponse executeByOrderId(Long orderId) {
        List<DroneMission> missions = missionRepository.findByOrderId(orderId);

        if (missions.isEmpty()) {
            throw new MissionNotFoundException(orderId);
        }

        // Get first mission (should only be one mission per order)
        return TrackingResponse.fromEntity(missions.get(0));
    }
}
