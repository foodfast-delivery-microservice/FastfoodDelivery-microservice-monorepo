package com.example.demo.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private Long merchantId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String address;

    private String city;

    private String district;

    private String image;

    private String phone;

    private String email;

    /**
     * Opening hours stored as a JSON/string: e.g. {"monday":"08:00-22:00",...}
     */
    private String openingHours;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean approved = Boolean.FALSE;

    /**
     * Restaurant category: FOOD/DRINK/BOTH/OTHER
     */
    private String category;

    private BigDecimal deliveryFee;

    private Integer estimatedDeliveryTime; // in minutes

    private Double rating;

    private Integer reviewCount;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

