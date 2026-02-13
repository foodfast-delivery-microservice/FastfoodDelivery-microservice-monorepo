package com.example.order_service.infrastructure.persistence.adapter;

import com.example.order_service.domain.valueobjects.PageRequest;
import com.example.order_service.domain.valueobjects.PageResult;
import org.springframework.data.domain.Page;

/**
 * Helper class to convert between Spring Data Page and domain PageResult.
 * This is infrastructure concern - domain layer doesn't know about Spring.
 */
public class PageResultConverter {
    
    public static <T> PageResult<T> toDomainPageResult(Page<T> springPage, PageRequest domainPageRequest) {
        return PageResult.of(
                springPage.getContent(),
                domainPageRequest,
                springPage.getTotalElements()
        );
    }
}
