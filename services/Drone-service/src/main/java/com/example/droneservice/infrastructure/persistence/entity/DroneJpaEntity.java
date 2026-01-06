package com.example.droneservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity for Drone persistence.
 * This is the infrastructure layer representation with JPA annotations.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "drones")
public class DroneJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String serialNumber;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private Integer batteryLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private String state;

    private Double currentLatitude;
    private Double currentLongitude;

    private Double baseLatitude;
    private Double baseLongitude;

    private Double weightCapacity;

    @OneToMany(mappedBy = "drone", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DroneMissionJpaEntity> missions = new ArrayList<>();
}
