package com.example.demo.infrastructure.messaging.event;

import java.io.Serializable;
import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MerchantActivatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    Long merchantId;
    Instant occurredAt;
    String reason;
    String triggeredBy;
}

