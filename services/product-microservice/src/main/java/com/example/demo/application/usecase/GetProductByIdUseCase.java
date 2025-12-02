package com.example.demo.application.usecase;

import com.example.demo.domain.exception.InvalidIdException;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.interfaces.rest.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
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
