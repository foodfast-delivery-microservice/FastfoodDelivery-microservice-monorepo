package com.example.productservice.infrastructure.persistence.mapper;

import com.example.productservice.domain.entities.Product;
import com.example.productservice.domain.valueobjects.Price;
import com.example.productservice.domain.valueobjects.Stock;
import com.example.productservice.infrastructure.persistence.entity.ProductJpaEntity;

/**
 * Mapper to convert between Product domain entity and ProductJpaEntity.
 */
public class ProductMapper {

    /**
     * Convert domain entity to JPA entity
     */
    public static ProductJpaEntity toJpaEntity(Product domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        ProductJpaEntity jpaEntity = new ProductJpaEntity();
        jpaEntity.setId(domainEntity.getId());
        jpaEntity.setName(domainEntity.getName());
        jpaEntity.setDescription(domainEntity.getDescription());
        // Convert Price value object to BigDecimal
        jpaEntity.setPrice(domainEntity.getPrice() != null ? domainEntity.getPrice().getAmount() : null);
        // Convert Stock value object to Integer
        jpaEntity.setStock(domainEntity.getStock() != null ? domainEntity.getStock().getQuantity() : null);
        jpaEntity.setCategory(domainEntity.getCategory());
        jpaEntity.setMerchantId(domainEntity.getMerchantId());
        jpaEntity.setActive(domainEntity.isActive());
        jpaEntity.setImageUrl(domainEntity.getImageUrl());

        return jpaEntity;
    }

    /**
     * Convert JPA entity to domain entity
     */
    public static Product toDomainEntity(ProductJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        Product domainEntity = new Product();
        domainEntity.setId(jpaEntity.getId());
        domainEntity.setName(jpaEntity.getName());
        domainEntity.setDescription(jpaEntity.getDescription());
        // Convert BigDecimal to Price value object
        domainEntity.setPrice(jpaEntity.getPrice() != null ? new Price(jpaEntity.getPrice()) : null);
        // Convert Integer to Stock value object
        domainEntity.setStock(jpaEntity.getStock() != null ? new Stock(jpaEntity.getStock()) : null);
        domainEntity.setCategory(jpaEntity.getCategory());
        domainEntity.setMerchantId(jpaEntity.getMerchantId());
        domainEntity.setActive(jpaEntity.isActive());
        domainEntity.setImageUrl(jpaEntity.getImageUrl());

        return domainEntity;
    }
}
