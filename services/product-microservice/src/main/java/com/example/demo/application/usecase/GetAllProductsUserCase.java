package com.example.demo.application.usecase;

import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.interfaces.rest.dto.ProductResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetAllProductsUserCase {
    private final ProductRepository productRepository;

    public List<ProductResponse> getAllProducts(Long merchantId) {
        List<com.example.demo.domain.model.Product> products;
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
