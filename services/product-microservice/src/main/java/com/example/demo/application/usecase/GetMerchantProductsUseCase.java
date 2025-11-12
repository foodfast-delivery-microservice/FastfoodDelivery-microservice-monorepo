package com.example.demo.application.usecase;

import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.interfaces.rest.dto.ProductResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetMerchantProductsUseCase {

    private final ProductRepository productRepository;

    public List<ProductResponse> execute(Long merchantId, boolean includeInactive) {
        return productRepository.findByMerchantId(merchantId)
                .stream()
                .filter(product -> includeInactive || product.isActive())
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }
}

