package com.example.productservice.infrastructure.persistence.adapter;

import com.example.productservice.domain.entities.Product;
import com.example.productservice.domain.repository.ProductRepository;
import com.example.productservice.infrastructure.persistence.entity.ProductJpaEntity;
import com.example.productservice.infrastructure.persistence.mapper.ProductMapper;
import com.example.productservice.infrastructure.persistence.repository.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing domain ProductRepository using JPA infrastructure.
 * Bridges the gap between domain layer and infrastructure layer.
 */
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id)
                .map(ProductMapper::toDomainEntity);
    }

    @Override
    public Optional<Product> findByName(String name) {
        return productJpaRepository.findByName(name)
                .map(ProductMapper::toDomainEntity);
    }

    @Override
    public Optional<Product> findByNameIgnoreCase(String name) {
        return productJpaRepository.findByNameIgnoreCase(name)
                .map(ProductMapper::toDomainEntity);
    }

    @Override
    public Optional<Product> findByNameIgnoreCaseAndMerchantId(String name, Long merchantId) {
        return productJpaRepository.findByNameIgnoreCaseAndMerchantId(name, merchantId)
                .map(ProductMapper::toDomainEntity);
    }

    @Override
    public Optional<Product> findByIdAndMerchantId(Long id, Long merchantId) {
        return productJpaRepository.findByIdAndMerchantId(id, merchantId)
                .map(ProductMapper::toDomainEntity);
    }

    @Override
    public Optional<Product> findByIdAndMerchantIdWithLock(Long id, Long merchantId) {
        return productJpaRepository.findByIdAndMerchantIdWithLock(id, merchantId)
                .map(ProductMapper::toDomainEntity);
    }

    @Override
    public List<Product> findByCategory(Product.Category category) {
        return productJpaRepository.findByCategory(category).stream()
                .map(ProductMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByMerchantId(Long merchantId) {
        return productJpaRepository.findByMerchantId(merchantId).stream()
                .map(ProductMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByMerchantIdAndActiveTrue(Long merchantId) {
        return productJpaRepository.findByMerchantIdAndActiveTrue(merchantId).stream()
                .map(ProductMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByActiveTrue() {
        return productJpaRepository.findByActiveTrue().stream()
                .map(ProductMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByCategoryAndActiveTrue(Product.Category category) {
        return productJpaRepository.findByCategoryAndActiveTrue(category).stream()
                .map(ProductMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity jpaEntity = ProductMapper.toJpaEntity(product);
        ProductJpaEntity saved = productJpaRepository.save(jpaEntity);
        return ProductMapper.toDomainEntity(saved);
    }

    @Override
    public void delete(Product product) {
        ProductJpaEntity jpaEntity = ProductMapper.toJpaEntity(product);
        productJpaRepository.delete(jpaEntity);
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return productJpaRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public void deactivateProductsByMerchantId(Long merchantId) {
        productJpaRepository.deactivateProductsByMerchantId(merchantId);
    }

    @Override
    public int reactivateProductsByMerchantId(Long merchantId) {
        return productJpaRepository.reactivateProductsByMerchantId(merchantId);
    }
}
