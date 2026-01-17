package com.example.productservice.domain.repository;

import com.example.productservice.domain.entities.Product;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for Product.
 * This is a pure domain interface with no framework dependencies.
 * Implementations are in the infrastructure layer.
 */
public interface ProductRepository {
    
    Optional<Product> findById(Long id);
    
    Optional<Product> findByName(String name);
    
    Optional<Product> findByNameIgnoreCase(String name);
    
    Optional<Product> findByNameIgnoreCaseAndMerchantId(String name, Long merchantId);
    
    Optional<Product> findByIdAndMerchantId(Long id, Long merchantId);
    
    Optional<Product> findByIdAndMerchantIdWithLock(Long id, Long merchantId);
    
    List<Product> findByCategory(Product.Category category);
    
    List<Product> findByMerchantId(Long merchantId);
    
    List<Product> findByMerchantIdAndActiveTrue(Long merchantId);
    
    List<Product> findByActiveTrue();
    
    List<Product> findByCategoryAndActiveTrue(Product.Category category);
    
    Product save(Product product);
    
    void delete(Product product);
    
    boolean existsByNameIgnoreCase(String name);
    
    void deactivateProductsByMerchantId(Long merchantId);
    
    int reactivateProductsByMerchantId(Long merchantId);
}
