package com.example.demo.application.ProductUseCase;

import com.example.demo.domain.exception.InvalidNameException;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteProductByNameUseCase {
    private final ProductRepository productRepository;

    public String deleteProductByName(String name) {
        Product product = productRepository.findByNameIgnoreCase(name)
                .orElseThrow(()-> new InvalidNameException());
        productRepository.delete(product);
        return "Product with name " + name + " has been deleted";

    }
}
