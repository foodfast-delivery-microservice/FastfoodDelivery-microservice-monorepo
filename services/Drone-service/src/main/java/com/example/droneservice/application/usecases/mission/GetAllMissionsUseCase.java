package com.example.droneservice.application.usecases.mission;

import com.example.droneservice.application.DTOs.mission.MissionResponse;
import com.example.droneservice.domain.repository.DroneMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAllMissionsUseCase {
    private final DroneMissionRepository droneMissionRepository;

    public List<MissionResponse> execute(){
        return droneMissionRepository.findAll()
                .stream()
                .map(MissionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
