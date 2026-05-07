package com.example.userservice.application.DTOs.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateEmailDeliverabilityRequest {
    @NotNull
    private Boolean undeliverable;
    private java.time.LocalDateTime bouncedAt;
    private Integer bounceIncrement;
}
