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
public class MerchantActivatedEvent {
    private Long merchantId;
    private Instant occurredAt;
    private String reason;
    private String triggeredBy;
}
