package com.example.demo.application.usecase;

import com.example.demo.domain.exception.InvalidIdException;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.interfaces.rest.dto.ProductPatch;
import com.example.demo.interfaces.rest.dto.ProductResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateProductUseCase {
    private final ProductRepository productRepository;

    public ProductResponse updateProduct(Long productId, ProductPatch productPatch, Long merchantId, boolean isAdmin) {
        Product existingProduct;
        if (isAdmin) {
            existingProduct = productRepository.findById(productId)
                    .orElseThrow(() -> new InvalidIdException("Invalid ID"));
        } else {
            if (merchantId == null) {
                throw new IllegalArgumentException("merchantId is required");
            }
            existingProduct = productRepository.findByIdAndMerchantId(productId, merchantId)
                    .orElseThrow(() -> new InvalidIdException("Invalid ID or no permission"));
        }

        // only update when field was sent (not null)
        if (productPatch.getName() != null) {
            existingProduct.setName(productPatch.getName());
        }
        if (productPatch.getDescription() != null) {
            existingProduct.setDescription(productPatch.getDescription());
        }
        if (productPatch.getPrice() != null) {
            existingProduct.setPrice(productPatch.getPrice());
        }
        if (productPatch.getStock() != null) {
            existingProduct.setStock(productPatch.getStock());
        }
        if (productPatch.getCategory() != null) {
            existingProduct.setCategory(productPatch.getCategory());
        }
        if (productPatch.getActive() != null) {
            existingProduct.setActive(productPatch.getActive());
        }
        if (productPatch.getImageUrl() != null) {
            existingProduct.setImageUrl(productPatch.getImageUrl());
        }
        Product saved = productRepository.save(existingProduct);
        return ProductResponse.fromEntity(saved);
    }
}
