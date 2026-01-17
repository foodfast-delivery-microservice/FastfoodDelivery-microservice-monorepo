package com.example.productservice.application.usecases.product;

import com.example.productservice.application.DTOs.ProductRequest;
import com.example.productservice.application.DTOs.ProductResponse;
import com.example.productservice.domain.entities.Product;
import com.example.productservice.domain.exception.InvalidCategoryException;
import com.example.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class CreateProductUseCase {
    private final ProductRepository productRepository;

    public ProductResponse create(ProductRequest productRequest) {

        // check category
        Product.Category category;
        try {
            category = Product.Category.valueOf(productRequest.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCategoryException(productRequest.getCategory());
        }

        if (productRequest.getMerchantId() == null) {
            throw new IllegalArgumentException("merchantId is required");
        }

        Optional<Product> existingProduct = productRepository.findByNameIgnoreCaseAndMerchantId(
                productRequest.getName(),
                productRequest.getMerchantId());

        Product product;
        if (existingProduct.isPresent()) {
            // nếu sản phẩm đã tồn tại cho merchant này thì cập nhật tồn kho & thông tin
            product = existingProduct.get();
            if (productRequest.getStock() != null) {
                product.setStock(product.getStock() + productRequest.getStock());
            }
            if (productRequest.getDescription() != null) {
                product.setDescription(productRequest.getDescription());
            }
            if (productRequest.getPrice() != null) {
                product.setPrice(productRequest.getPrice());
            }
            if (productRequest.getActive() != null) {
                product.setActive(productRequest.getActive());
            }
            if (productRequest.getImageUrl() != null) {
                product.setImageUrl(productRequest.getImageUrl());
            }
            product.setCategory(category);
        } else {
            product = new Product();
            product.setName(productRequest.getName());
            product.setDescription(productRequest.getDescription());
            product.setPrice(productRequest.getPrice());
            product.setStock(productRequest.getStock());
            product.setCategory(category);
            product.setMerchantId(productRequest.getMerchantId());
            product.setActive(productRequest.getActive() != null ? productRequest.getActive() : true);
            product.setImageUrl(productRequest.getImageUrl());
        }

        // ensure merchant id set (existing record already has it)
        if (product.getMerchantId() == null) {
            product.setMerchantId(productRequest.getMerchantId());
        }

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }
}
