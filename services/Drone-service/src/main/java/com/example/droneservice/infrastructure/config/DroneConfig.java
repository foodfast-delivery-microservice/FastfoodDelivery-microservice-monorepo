package com.example.droneservice.infrastructure.config;

import com.example.droneservice.application.usecases.drone.CreateDroneUseCase;
import com.example.droneservice.application.usecases.drone.GetAllDroneUseCase;
import com.example.droneservice.application.usecases.drone.GetDroneByIdUseCase;
import com.example.droneservice.application.usecases.drone.GetDroneByStateUseCase;
import com.example.droneservice.domain.repository.DroneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DroneConfig {
    private final DroneRepository droneRepository;

    @Bean
    public CreateDroneUseCase createDroneUseCase(DroneRepository droneRepository) {
        return new CreateDroneUseCase(droneRepository);
    }

    @Bean
    public GetAllDroneUseCase getAllDroneUseCase() {
        return new GetAllDroneUseCase(droneRepository);
    }

    @Bean
    public GetDroneByStateUseCase getDroneByStateUseCase() {
        return new GetDroneByStateUseCase(droneRepository);
    }

    @Bean
    public GetDroneByIdUseCase getDroneByIdUseCase() {
        return new GetDroneByIdUseCase(droneRepository);
    }
}
