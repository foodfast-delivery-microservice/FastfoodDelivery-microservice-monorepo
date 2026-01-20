package com.example.productservice.application.usecases.product;

import com.example.productservice.application.DTOs.ProductResponse;
import com.example.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetAllProductsUseCase {
    private final ProductRepository productRepository;

    public List<ProductResponse> getAllProducts(Long merchantId) {
        List<com.example.productservice.domain.entities.Product> products;
        if (merchantId != null) {
            products = productRepository.findByMerchantIdAndActiveTrue(merchantId);
        } else {
            products = productRepository.findByActiveTrue();
        }

        return products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
