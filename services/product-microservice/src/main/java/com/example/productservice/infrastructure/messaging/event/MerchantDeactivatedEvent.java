package com.example.productservice.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MerchantDeactivatedEvent {
    private Long merchantId;
    private Instant occurredAt;
    private String reason;
}
