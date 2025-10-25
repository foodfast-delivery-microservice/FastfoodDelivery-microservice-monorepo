package com.example.demo.application.ProductUseCase;

import com.example.demo.domain.exception.InvalidCategoryException;
import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.interfaces.rest.dto.ProductRequest;
import com.example.demo.interfaces.rest.dto.ProductResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateProductUseCase {
    private final ProductRepository productRepository;

    public ProductResponse create (ProductRequest productRequest) {

        Product.Category category;
        try {
            category = Product.Category.valueOf(productRequest.getCategory().toUpperCase());
        }catch (IllegalArgumentException e){
            throw new InvalidCategoryException(productRequest.getCategory());
        }

        Product product = new Product();
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setStock(productRequest.getStock());
        //
         // lát test xem điền khác category trong enum thì nó có nhận không
        //
        product.setCategory(category);
        product.setActive(true);

        Product saved = productRepository.save(product);
        return new ProductResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getPrice(),
                saved.getStock(),
                saved.getCategory().name(),
                saved.isActive());
    }

}
