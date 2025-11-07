package com.example.payment.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //Cho biết sự kiện này liên quan đến loại đối tượng nghiệp vụ nào (order, product,...)
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    //Vai trò: Cho biết ID cụ thể của đối tượng nghiệp vụ đó.
    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    //Vai trò: Tên chính xác của sự kiện đã xảy ra.
    //Ví dụ: "OrderCreatedEvent", "PaymentSuccessEvent", "PaymentFailedEvent".
    @Column(name = "type", nullable = false, length = 100)
    private String type;

    //Nó chứa toàn bộ dữ liệu bạn muốn gửi đi, thường được lưu dưới dạng chuỗi JSON.
    @Column(name = "payload", columnDefinition = "JSON", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.NEW;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markAsProcessed() {
        this.status = EventStatus.PROCESSED;
    }

    public void markAsFailed() {
        this.status = EventStatus.FAILED;
    }
}
