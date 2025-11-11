package com.example.payment.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "idempotency_keys")
@Data
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", unique = true) // 'unique = true' là một ý hay
    private Long orderId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    public IdempotencyKey(Long orderId, Long paymentId) {
        this.orderId = orderId;
        this.paymentId = paymentId;
    }
}

