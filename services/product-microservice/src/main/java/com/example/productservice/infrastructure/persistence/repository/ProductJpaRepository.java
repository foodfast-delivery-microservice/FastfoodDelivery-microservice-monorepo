package com.example.productservice.infrastructure.persistence.repository;

import com.example.productservice.domain.entities.Product;
import com.example.productservice.infrastructure.persistence.entity.ProductJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for ProductJpaEntity persistence.
 * This is the infrastructure layer repository with Spring Data JPA.
 */
@Repository
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {

    boolean existsByNameIgnoreCase(String name);
    
    Optional<ProductJpaEntity> findByName(String name);
    
    Optional<ProductJpaEntity> findByNameIgnoreCase(String name);
    
    List<ProductJpaEntity> findByCategory(Product.Category category);
    
    Optional<ProductJpaEntity> findByNameIgnoreCaseAndMerchantId(String name, Long merchantId);
    
    Optional<ProductJpaEntity> findByIdAndMerchantId(Long id, Long merchantId);
    
    // Method với pessimistic lock cho stock deduction/restoration
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductJpaEntity p WHERE p.id = :id AND p.merchantId = :merchantId")
    Optional<ProductJpaEntity> findByIdAndMerchantIdWithLock(@Param("id") Long id, @Param("merchantId") Long merchantId);
    
    List<ProductJpaEntity> findByMerchantId(Long merchantId);
    
    List<ProductJpaEntity> findByMerchantIdAndActiveTrue(Long merchantId);
    
    List<ProductJpaEntity> findByActiveTrue();
    
    List<ProductJpaEntity> findByCategoryAndActiveTrue(Product.Category category);

    @Modifying
    @Query("UPDATE ProductJpaEntity p SET p.active = false WHERE p.merchantId = :merchantId")
    void deactivateProductsByMerchantId(@Param("merchantId") Long merchantId);

    @Modifying
    @Query("UPDATE ProductJpaEntity p SET p.active = true WHERE p.merchantId = :merchantId")
    int reactivateProductsByMerchantId(@Param("merchantId") Long merchantId);
}
