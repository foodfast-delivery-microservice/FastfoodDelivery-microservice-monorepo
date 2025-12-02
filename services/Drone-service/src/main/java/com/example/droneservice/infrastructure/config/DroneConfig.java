package com.example.droneservice.infrastructure.config;

import com.example.droneservice.application.usecase.CreateDroneUseCase;
import com.example.droneservice.application.usecase.GetAllDroneUseCase;
import com.example.droneservice.application.usecase.GetDroneByIdUseCase;
import com.example.droneservice.application.usecase.GetDroneByStateUseCase;
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
