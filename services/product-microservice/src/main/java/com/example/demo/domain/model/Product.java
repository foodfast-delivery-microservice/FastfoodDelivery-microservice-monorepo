package com.example.demo.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@EqualsAndHashCode(of = {"id"}) //Chỉ sử dụng field id để so sánh và tính hash, bỏ qua field khác.
@Table(
        name ="products"
        //indexes = { @Index(name = "idx_products_name", columnList = "name") }, loc theo name tang toc do tim kiem
        //uniqueConstraints = {@UniqueConstraint(columnNames ={"name"} )}  dam bao 2 sp khong trung ten
)

public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    @NotBlank
    private String name;


    @Column(nullable = false)
    @NotBlank
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "price must be > 0")
    private BigDecimal price;

    @Column(nullable = false)
    @Min(0)
    private Integer stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)

    private Category category;

    @Column(nullable = false)
    private boolean active;

    public enum Category {
        DRINK,
        FOOD
    }
}
