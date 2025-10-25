package com.example.demo.domain.repository;

import com.example.demo.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {

    // kt ten ton tai khong va khong tinh viet hoa hay viet thuong
    boolean existsByNameIgnoreCase(String name);

    Optional<Product> findByNameIgnoreCase(String name);
    List<Product> findByCategory(Product.Category category);
}
