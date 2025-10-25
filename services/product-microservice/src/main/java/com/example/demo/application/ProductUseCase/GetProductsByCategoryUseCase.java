package com.example.demo.application.ProductUseCase;

import com.example.demo.domain.exception.InvalidCategoryException;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.interfaces.rest.dto.ProductResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetProductsByCategoryUseCase {
    private final ProductRepository productRepository;

    public List<ProductResponse> getProductsByCategory(String category) {
        Product.Category categoryEnum;
        // chuyển từ enum sang string
        try{
            categoryEnum = Product.Category.valueOf(category.toUpperCase());
        }catch(IllegalArgumentException e){
            throw new InvalidCategoryException(category);
        }
        return productRepository.findByCategory(categoryEnum)
                .stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
