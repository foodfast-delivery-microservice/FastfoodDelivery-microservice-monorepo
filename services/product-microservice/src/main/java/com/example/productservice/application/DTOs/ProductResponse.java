package com.example.productservice.application.DTOs;

import com.example.productservice.domain.entities.Product;
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
        ProductResponse productResponse = new ProductResponse();
        productResponse.setId(product.getId());
        productResponse.setName(product.getName());
        productResponse.setDescription(product.getDescription());
        // Convert Price value object to BigDecimal
        productResponse.setPrice(product.getPrice() != null ? product.getPrice().getAmount() : null);
        // Convert Stock value object to Integer
        productResponse.setStock(product.getStock() != null ? product.getStock().getQuantity() : null);
        productResponse.setCategory(product.getCategory().name());
        productResponse.setActive(product.isActive());
        productResponse.setMerchantId(product.getMerchantId());
        productResponse.setImageUrl(product.getImageUrl());
        return productResponse;
    }
}
