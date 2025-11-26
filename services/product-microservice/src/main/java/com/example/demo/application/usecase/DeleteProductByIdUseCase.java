package com.example.demo.application.usecase;

import com.example.demo.domain.exception.InvalidIdException;
import com.example.demo.domain.exception.ProductDeletionNotAllowedException;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteProductByIdUseCase {
    private final ProductRepository productRepository;

    public void deleteProduct(Long id, Long merchantId, boolean isAdmin) {
        Product product;
        if (isAdmin) {
            product = productRepository.findById(id)
                    .orElseThrow(() -> new InvalidIdException("Invalid ID"));
        } else {
            if (merchantId == null) {
                throw new IllegalArgumentException("merchantId is required");
            }
            product = productRepository.findByIdAndMerchantId(id, merchantId)
                    .orElseThrow(() -> new InvalidIdException("Invalid ID or no permission"));
        }

        // Chỉ cho phép xóa khi sản phẩm đã được "vô hiệu hóa" (active = false)
        if (product.isActive()) {
            throw new ProductDeletionNotAllowedException("Product must be inactive (active = false) before deletion");
        }

        productRepository.delete(product);
    }
}
