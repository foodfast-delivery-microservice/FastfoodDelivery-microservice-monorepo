package com.example.productservice.application.usecases.product;

import com.example.productservice.application.DTOs.ProductResponse;
import com.example.productservice.domain.entities.Product;
import com.example.productservice.domain.exception.InvalidIdException;
import com.example.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetProductByIdUseCase {
    private final ProductRepository productRepository;

    public ProductResponse execute(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new InvalidIdException("Product not found"));

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory().name(),
                product.isActive(),
                product.getMerchantId(),
                product.getImageUrl());
    }
}
