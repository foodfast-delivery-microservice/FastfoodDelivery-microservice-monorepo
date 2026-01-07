package com.example.droneservice.infrastructure.persistence.entity;

import com.example.droneservice.domain.valueobjects.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA Entity for DroneMission persistence.
 * This is the infrastructure layer representation with JPA annotations.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "drone_missions")
public class DroneMissionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drone_id", nullable = false)
    private DroneJpaEntity drone;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Double pickupLatitude;

    @Column(nullable = false)
    private Double pickupLongitude;

    @Column(nullable = false)
    private Double deliveryLatitude;

    @Column(nullable = false)
    private Double deliveryLongitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
