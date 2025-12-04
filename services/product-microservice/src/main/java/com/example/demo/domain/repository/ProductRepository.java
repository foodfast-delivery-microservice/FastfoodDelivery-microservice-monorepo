package com.example.demo.domain.repository;

import com.example.demo.domain.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {

    // kt ten ton tai khong va khong tinh viet hoa hay viet thuong
    boolean existsByNameIgnoreCase(String name);
    Optional<Product> findByName(String name);
    Optional<Product> findByNameIgnoreCase(String name);
    List<Product> findByCategory(Product.Category category);
    Optional<Product> findByNameIgnoreCaseAndMerchantId(String name, Long merchantId);
    Optional<Product> findByIdAndMerchantId(Long id, Long merchantId);
    
    // Method với pessimistic lock cho stock deduction/restoration
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.merchantId = :merchantId")
    Optional<Product> findByIdAndMerchantIdWithLock(@Param("id") Long id, @Param("merchantId") Long merchantId);
    
    List<Product> findByMerchantId(Long merchantId);
    List<Product> findByMerchantIdAndActiveTrue(Long merchantId);
    List<Product> findByActiveTrue();
    List<Product> findByCategoryAndActiveTrue(Product.Category category);

    @Modifying
    @Query("UPDATE Product p SET p.active = false WHERE p.merchantId = :merchantId")
    void deactivateProductsByMerchantId(@Param("merchantId") Long merchantId);

    @Modifying
    @Query("UPDATE Product p SET p.active = true WHERE p.merchantId = :merchantId")
    int reactivateProductsByMerchantId(@Param("merchantId") Long merchantId);

    @Modifying
    @Query("DELETE FROM Product p WHERE p.merchantId = :merchantId")
    void deleteProductsByMerchantId(@Param("merchantId") Long merchantId);
}
