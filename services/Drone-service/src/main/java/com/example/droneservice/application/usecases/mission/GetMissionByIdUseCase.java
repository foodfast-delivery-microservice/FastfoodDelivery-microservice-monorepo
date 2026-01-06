package com.example.droneservice.application.usecases.mission;

import com.example.droneservice.application.DTOs.mission.MissionResponse;
import com.example.droneservice.domain.entities.DroneMission;
import com.example.droneservice.domain.exception.InvalidId;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMissionByIdUseCase {
    private final DroneMissionRepository droneMissionRepository;

    public MissionResponse execute(Long id){
        DroneMission droneMission = droneMissionRepository.findById(id)
                .orElseThrow(()-> new InvalidId(id));
        return MissionResponse.fromEntity(droneMission);
    }
}
