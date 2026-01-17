package com.example.productservice.application.usecases.product;

import com.example.productservice.application.DTOs.ProductResponse;
import com.example.productservice.domain.entities.Product;
import com.example.productservice.domain.exception.InvalidCategoryException;
import com.example.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetProductsByCategoryUseCase {
    private final ProductRepository productRepository;

    public List<ProductResponse> getProductsByCategory(String category) {
        Product.Category categoryEnum;
        // chuyển từ enum sang string
        try {
            categoryEnum = Product.Category.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCategoryException(category);
        }
        return productRepository.findByCategoryAndActiveTrue(categoryEnum)
                .stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
