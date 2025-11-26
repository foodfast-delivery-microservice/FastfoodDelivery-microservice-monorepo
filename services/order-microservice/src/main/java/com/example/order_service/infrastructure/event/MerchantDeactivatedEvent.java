package com.example.order_service.infrastructure.event;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MerchantDeactivatedEvent {
    private Long merchantId;
    private Instant occurredAt;
    private String reason;
}

