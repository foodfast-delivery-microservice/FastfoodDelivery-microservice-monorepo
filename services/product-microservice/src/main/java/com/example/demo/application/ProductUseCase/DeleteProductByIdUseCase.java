package com.example.demo.application.ProductUseCase;

import com.example.demo.domain.exception.InvalidIdException;
import com.example.demo.domain.exception.InvalidNameException;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class DeleteProductByIdUseCase {
    private final ProductRepository productRepository;

    public void deleteProductByName(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(()-> new InvalidIdException("Invalid ID"));
        productRepository.delete(product);


    }
}
