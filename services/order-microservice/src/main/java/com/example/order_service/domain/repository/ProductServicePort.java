package com.example.order_service.domain.repository;


import com.example.order_service.application.dto.ProductValidationRequest;
import com.example.order_service.application.dto.ProductValidationResponse;

import java.util.List;

public interface ProductServicePort {
    List<ProductValidationResponse> validateProducts(List<ProductValidationRequest> request);
}
