package com.example.demo.interfaces.rest.dto;

import com.example.demo.domain.model.Product;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String category;
    private boolean active;
    private Long merchantId;
    private String imageUrl;

    public static ProductResponse fromEntity(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        ProductResponse productResponse = new ProductResponse();
        productResponse.setId(product.getId());
        productResponse.setName(product.getName());
        productResponse.setDescription(product.getDescription());
        productResponse.setPrice(product.getPrice());
        productResponse.setStock(product.getStock());
        productResponse.setCategory(product.getCategory() != null ? product.getCategory().name() : null);
        productResponse.setActive(product.isActive());
        productResponse.setMerchantId(product.getMerchantId());
        productResponse.setImageUrl(product.getImageUrl());
        return productResponse;
    }
}
