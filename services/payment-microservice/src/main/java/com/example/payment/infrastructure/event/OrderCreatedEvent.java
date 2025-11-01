package com.example.payment.infrastructure.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor // Cần constructor rỗng để Jackson deserialize
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private BigDecimal grandTotal;
    private String currency;
}
