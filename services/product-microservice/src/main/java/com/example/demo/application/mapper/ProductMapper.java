package com.example.demo.application.mapper;

import com.example.demo.interfaces.rest.dto.ProductResponse;
import com.example.demo.domain.model.Product;

public interface ProductMapper {
    ProductResponse toResponse(Product p);
}
