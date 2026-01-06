package com.example.droneservice.application.usecases.mission;

import com.example.droneservice.application.DTOs.mission.MissionResponse;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for getting all missions by drone ID
 */
@Service
@RequiredArgsConstructor
public class GetMissionsByDroneIdUseCase {
    private final DroneMissionRepository missionRepository;

    public List<MissionResponse> execute(Long droneId) {
        return missionRepository.findByDroneId(droneId).stream()
                .map(MissionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
