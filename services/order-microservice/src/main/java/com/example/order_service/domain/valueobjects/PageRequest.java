package com.example.order_service.domain.valueobjects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain value object for pagination requests.
 * This replaces Spring Data's Pageable in the domain layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    private int page;
    private int size;
    private String sortBy;
    private SortDirection sortDirection;

    public enum SortDirection {
        ASC, DESC
    }

    public int getOffset() {
        return page * size;
    }
}
