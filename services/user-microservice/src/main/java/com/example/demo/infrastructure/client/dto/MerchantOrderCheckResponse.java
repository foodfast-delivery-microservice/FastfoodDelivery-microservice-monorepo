package com.example.demo.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantOrderCheckResponse {
    private boolean canDelete;
    private long activeOrderCount;
    private List<String> activeStatuses;
}
