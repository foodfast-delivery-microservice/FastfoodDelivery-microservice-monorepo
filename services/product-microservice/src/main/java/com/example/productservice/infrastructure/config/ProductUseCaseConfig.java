package com.example.productservice.infrastructure.config;

import com.example.productservice.application.usecases.product.*;
import com.example.productservice.application.usecases.stock.*;
import com.example.productservice.application.usecases.validation.*;
import com.example.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class ProductUseCaseConfig {
    private final ProductRepository productRepository;

    @Bean
    public CreateProductUseCase createProduct() {
        return new CreateProductUseCase(productRepository);
    }

    @Bean
    public DeleteProductByIdUseCase deleteProductByNameUseCase() {
        return new DeleteProductByIdUseCase(productRepository);
    }

    @Bean
    public GetAllProductsUseCase getAllProductsUseCase() {
        return new GetAllProductsUseCase(productRepository);
    }

    @Bean
    public GetProductsByCategoryUseCase getProductsByCategoryUseCase() {
        return new GetProductsByCategoryUseCase(productRepository);
    }

    @Bean
    public ValidateProductsUseCase validateProductsUseCase() {
        return new ValidateProductsUseCase(productRepository);
    }

    @Bean
    public UpdateProductUseCase updateProductUseCase() {
        return new UpdateProductUseCase(productRepository);
    }

    @Bean
    public GetMerchantProductsUseCase getMerchantProductsUseCase() {
        return new GetMerchantProductsUseCase(productRepository);
    }

    @Bean
    public GetProductByIdUseCase getProductByIdUseCase() {
        return new GetProductByIdUseCase(productRepository);
    }

    @Bean
    public RestoreStockUseCase restoreStockUseCase() {
        return new RestoreStockUseCase(productRepository);
    }

    @Bean
    public DeductStockUseCase deductStockUseCase() {
        return new DeductStockUseCase(productRepository);
    }
}
