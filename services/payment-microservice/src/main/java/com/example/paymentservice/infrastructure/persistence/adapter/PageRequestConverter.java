package com.example.paymentservice.infrastructure.persistence.adapter;

import com.example.paymentservice.domain.valueobjects.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Helper class to convert between domain PageRequest and Spring Data Pageable.
 * This is infrastructure concern - domain layer doesn't know about Spring.
 */
public class PageRequestConverter {
    
    public static Pageable toSpringPageable(PageRequest domainPageRequest) {
        if (domainPageRequest == null) {
            return org.springframework.data.domain.PageRequest.of(0, 20);
        }
        
        Sort.Direction direction = domainPageRequest.getSortDirection() == PageRequest.SortDirection.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        
        Sort sort = domainPageRequest.getSortBy() != null
                ? Sort.by(direction, domainPageRequest.getSortBy())
                : Sort.unsorted();
        
        return org.springframework.data.domain.PageRequest.of(
                domainPageRequest.getPage(),
                domainPageRequest.getSize(),
                sort
        );
    }
}
