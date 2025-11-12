package com.example.demo.infracstructor.config;

import com.example.demo.application.usecase.*;
import com.example.demo.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class ProductUserConfig {
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
    public GetAllProductsUserCase getAllProductsUserCase() {
        return new GetAllProductsUserCase(productRepository);
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
}
