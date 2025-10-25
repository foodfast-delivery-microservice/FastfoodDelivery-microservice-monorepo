package com.example.demo.application.ProductUseCase;

import com.example.demo.domain.exception.InvalidNameException;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.interfaces.rest.dto.ProductPatch;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateProductUseCase {
    private final ProductRepository productRepository;

    public Product updateProduct(String name, ProductPatch productPatch) {
        Product existingProduct = productRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new InvalidNameException());

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
        if (productPatch.isActive()) {
            existingProduct.setActive(productPatch.isActive());
        }
        return productRepository.save(existingProduct);
    }
}
