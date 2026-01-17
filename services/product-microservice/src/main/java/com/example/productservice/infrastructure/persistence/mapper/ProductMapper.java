package com.example.productservice.infrastructure.persistence.mapper;

import com.example.productservice.domain.entities.Product;
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
        jpaEntity.setPrice(domainEntity.getPrice());
        jpaEntity.setStock(domainEntity.getStock());
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
        domainEntity.setPrice(jpaEntity.getPrice());
        domainEntity.setStock(jpaEntity.getStock());
        domainEntity.setCategory(jpaEntity.getCategory());
        domainEntity.setMerchantId(jpaEntity.getMerchantId());
        domainEntity.setActive(jpaEntity.isActive());
        domainEntity.setImageUrl(jpaEntity.getImageUrl());

        return domainEntity;
    }
}
