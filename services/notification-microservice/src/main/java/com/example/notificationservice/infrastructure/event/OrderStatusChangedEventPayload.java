package com.example.notificationservice.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEventPayload {
    private Long orderId;
    private String orderCode;
    private Long userId;
    private String oldStatus;
    private String newStatus;
    private String note;
    private String timestamp;
}
