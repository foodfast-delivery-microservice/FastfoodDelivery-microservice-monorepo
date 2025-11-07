package com.example.demo.application.usecase;

import com.example.demo.domain.exception.InvalidIdException;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteProductByIdUseCase {
    private final ProductRepository productRepository;

    public void deleteProductByName(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(()-> new InvalidIdException("Invalid ID"));
        productRepository.delete(product);


    }
}
