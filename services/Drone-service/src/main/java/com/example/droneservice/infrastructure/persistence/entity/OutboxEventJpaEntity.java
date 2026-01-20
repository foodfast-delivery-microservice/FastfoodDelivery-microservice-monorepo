package com.example.droneservice.infrastructure.persistence.entity;

import com.example.droneservice.domain.valueobjects.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA Entity for OutboxEvent persistence.
 * This is the infrastructure layer representation with JPA annotations.
 * Used for transactional outbox pattern to ensure reliable event publishing.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "type", nullable = false, length = 100)
    private String type;

    @Column(name = "payload", columnDefinition = "JSON", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status = EventStatus.NEW;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Set creation timestamp before persist
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
