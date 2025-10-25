package com.example.demo.application.ProductUseCase;

import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.interfaces.rest.dto.ProductResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetAllProductsUserCase {
    private final ProductRepository productRepository;

    public List<ProductResponse> getAllProducts(){
        return productRepository.findAll()
                .stream()
                .map(ProductResponse :: fromEntity)
                .collect(Collectors.toList());

    }
}
