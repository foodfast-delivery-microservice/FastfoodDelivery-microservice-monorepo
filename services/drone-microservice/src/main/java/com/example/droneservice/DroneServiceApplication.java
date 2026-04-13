package com.example.droneservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class DroneServiceApplication {

    public static void main(String[] args) {
        log.info("🚀 Starting Drone Service Application...");
        log.info("📅 Scheduling enabled - Drone simulation will run every 2 seconds");
        SpringApplication.run(DroneServiceApplication.class, args);
        log.info("✅ Drone Service Application started successfully");
    }

}