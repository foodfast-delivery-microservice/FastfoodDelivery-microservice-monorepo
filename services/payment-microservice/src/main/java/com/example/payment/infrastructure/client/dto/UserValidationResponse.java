package com.example.payment.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserValidationResponse {
    private Long userId;
    private boolean exists;
    private boolean active;
    private String username;
}

