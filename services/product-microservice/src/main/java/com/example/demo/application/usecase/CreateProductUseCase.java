package com.example.demo.application.usecase;

import com.example.demo.domain.exception.InvalidCategoryException;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.interfaces.rest.dto.ProductRequest;
import com.example.demo.interfaces.rest.dto.ProductResponse;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class CreateProductUseCase {
    private final ProductRepository productRepository;

    public ProductResponse create (ProductRequest productRequest) {

        // check category
        Product.Category category;
        try {
            category = Product.Category.valueOf(productRequest.getCategory().toUpperCase());
        }catch (IllegalArgumentException e){
            throw new InvalidCategoryException(productRequest.getCategory());
        }

        Optional<Product> existingProduct = productRepository.findByName(productRequest.getName());

        Product product;
        if (existingProduct.isPresent()) {
            // nếu sản phẩm đã tồn tại thì chỉ cần + stcock
            product = existingProduct.get();
            product.setStock(product.getStock() + productRequest.getStock());
        } else {
            product = new Product();
            product.setName(productRequest.getName());
            product.setDescription(productRequest.getDescription());
            product.setPrice(productRequest.getPrice());
            product.setStock(productRequest.getStock());
            product.setCategory(category);
            product.setActive(true);
        }


        Product saved = productRepository.save(product);
        return new ProductResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getPrice(),
                saved.getStock(),
                saved.getCategory().name(),
                saved.isActive());
    }

}
