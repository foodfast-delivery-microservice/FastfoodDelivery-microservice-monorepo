package com.example.notificationservice.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateEmailDeliverabilityRequest {
    private Boolean undeliverable;
    private java.time.LocalDateTime bouncedAt;
    private Integer bounceIncrement;
}
