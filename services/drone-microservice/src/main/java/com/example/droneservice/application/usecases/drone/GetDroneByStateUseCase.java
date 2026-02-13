package com.example.droneservice.application.usecases.drone;

import com.example.droneservice.application.DTOs.drone.DroneResponse;
import com.example.droneservice.domain.entities.Drone;
import com.example.droneservice.domain.valueobjects.State;
import com.example.droneservice.domain.repository.DroneRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetDroneByStateUseCase {
    private final DroneRepository droneRepository;

    public List<DroneResponse> execute(String stateName) {
        // 1. Convert từ String sang Enum
        State stateEnum;
        try {
            // BƯỚC QUAN TRỌNG: Convert String "IDLE" -> Enum State.IDLE
            // Nếu không có dòng này, bạn ném String vào Repo sẽ bị lỗi như trên
            stateEnum = State.valueOf(stateName.toUpperCase());

        } catch (IllegalArgumentException | NullPointerException e) {
            // Xử lý trường hợp gửi lên state tào lao (vd: "BAY_LUNG_TUNG")
            throw new IllegalArgumentException("Invalid drone state: " + stateName);
        }

        // Lúc này stateEnum đã là kiểu State, ném vào Repo là đúng khớp
        List<Drone> droneByState = droneRepository.findByState(stateEnum);

        return droneByState.stream()
                .map(DroneResponse::fromEntity) // Áp dụng hàm fromEntity cho từng con drone
                .collect(Collectors.toList());  // Gom lại thành List
    }
}
