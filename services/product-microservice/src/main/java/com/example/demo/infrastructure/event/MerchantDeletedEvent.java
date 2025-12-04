package com.example.demo.infrastructure.event;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MerchantDeletedEvent {
    private Long merchantId;
    private Instant occurredAt;
    private String reason;
}

