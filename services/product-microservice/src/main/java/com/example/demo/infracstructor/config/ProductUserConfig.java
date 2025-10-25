package com.example.demo.infracstructor.config;

import com.example.demo.application.ProductUseCase.CreateProductUseCase;
import com.example.demo.application.ProductUseCase.DeleteProductByNameUseCase;
import com.example.demo.application.ProductUseCase.GetAllProductsUserCase;
import com.example.demo.application.ProductUseCase.GetProductsByCategoryUseCase;
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
    public DeleteProductByNameUseCase deleteProductByNameUseCase() {
        return new DeleteProductByNameUseCase(productRepository);
    }

    @Bean
    public GetAllProductsUserCase getAllProductsUserCase() {
        return new GetAllProductsUserCase(productRepository);
    }

    @Bean
    public GetProductsByCategoryUseCase getProductsByCategoryUseCase() {
        return new GetProductsByCategoryUseCase(productRepository);
    }
}
