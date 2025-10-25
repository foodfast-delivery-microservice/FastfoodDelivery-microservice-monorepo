package com.example.demo.interfaces.rest;


import com.example.demo.application.ProductUseCase.CreateProductUseCase;
import com.example.demo.application.ProductUseCase.DeleteProductByNameUseCase;
import com.example.demo.application.ProductUseCase.GetAllProductsUserCase;
import com.example.demo.application.ProductUseCase.GetProductsByCategoryUseCase;
import com.example.demo.interfaces.common.ApiResponse;
import com.example.demo.interfaces.rest.dto.ProductRequest;
import com.example.demo.interfaces.rest.dto.ProductResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final DeleteProductByNameUseCase deleteProductByNameUseCase;
    private final GetAllProductsUserCase getAllProductsUserCase;
    private final GetProductsByCategoryUseCase getProductsByCategoryUseCase;


    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest productRequest) {
        ProductResponse created = createProductUseCase.create(productRequest);
        ApiResponse<ProductResponse> result =new ApiResponse<>(
                HttpStatus.CREATED,
                "created product",
                created,
                null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{nameRequest}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable String nameRequest) {
        deleteProductByNameUseCase.deleteProductByName(nameRequest);
        ApiResponse<String> result =new ApiResponse<>(
                HttpStatus.ACCEPTED,
                "deleted product",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);

    }
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {

        ApiResponse<List<ProductResponse>> result =new ApiResponse<>(
                HttpStatus.OK,
                "get all products",
                getAllProductsUserCase.getAllProducts(),
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
    @GetMapping("/{category}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(@PathVariable String category) {
        ApiResponse<List<ProductResponse>> result =new ApiResponse<>(
                HttpStatus.OK,
                "get products",
                getProductsByCategoryUseCase.getProductsByCategory(category),
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
    @GetMapping("/ping")
    public String ping() {
        return "product-service OK";
    }


}
