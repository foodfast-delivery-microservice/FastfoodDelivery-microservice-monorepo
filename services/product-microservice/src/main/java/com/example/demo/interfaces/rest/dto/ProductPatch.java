package com.example.demo.interfaces.rest.dto;

import com.example.demo.domain.model.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ProductPatch {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Product.Category category;
    private boolean active;
}
