package com.example.droneservice.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "drones")
public class Drone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    @Min(value = 0, message = "Battery level must be >= 0")
    @Max(value = 100, message = "Battery level must be <= 100")
    private int batteryLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state;

    @Min(-90)
    @Max(90)
    private Double currentLatitude;

    @Min(-180)
    @Max(180)
    private Double currentLongitude;

    @Min(-90)
    @Max(90)
    private Double baseLatitude;

    @Min(-180)
    @Max(180)
    private Double baseLongitude;

    @Max(5)
    private Double weightCapacity;

    @OneToMany(mappedBy = "drone", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DroneMission> missions = new ArrayList<>();
}
