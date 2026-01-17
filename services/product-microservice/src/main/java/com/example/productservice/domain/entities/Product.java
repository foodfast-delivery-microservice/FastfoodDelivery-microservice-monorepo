package com.example.productservice.domain.entities;

import lombok.*;

import java.math.BigDecimal;

/**
 * Pure domain entity representing a Product.
 * Contains business logic and domain rules, independent of persistence framework.
 * No JPA annotations - this is a pure business object.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "id" })
public class Product {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Category category;
    private Long merchantId;
    private boolean active = true;
    private String imageUrl;

    public enum Category {
        DRINK,
        FOOD
    }
}
