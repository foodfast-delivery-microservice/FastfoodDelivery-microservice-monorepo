package com.example.productservice.application.usecases.product;

import com.example.productservice.application.DTOs.ProductResponse;
import com.example.productservice.domain.entities.Product;
import com.example.productservice.domain.exception.InvalidIdException;
import com.example.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetMerchantProductsUseCase {

    private final ProductRepository productRepository;

    public List<ProductResponse> execute(Long merchantId, boolean includeInactive) {
        // Kiểm tra nếu merchantId null
        if (merchantId == null) {
            throw new InvalidIdException("merchantId is required");
        }

        List<Product> products = productRepository.findByMerchantId(merchantId);
        if (products.isEmpty()) {
            throw new InvalidIdException("No products found for merchant ID: " + merchantId);
        }
        return products.stream()
                .filter(product -> includeInactive || product.isActive())
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
