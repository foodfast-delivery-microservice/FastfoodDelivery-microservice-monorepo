package com.example.order_service.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persisted address that has already been normalized with AddressKit
 * and optionally adjusted on the map by user/driver.
 */
@Entity
@Table(name = "user_addresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "street", nullable = false, length = 255)
    private String street;

    @Column(name = "province_code", nullable = false, length = 20)
    private String provinceCode;

    @Column(name = "province_name", nullable = false, length = 100)
    private String provinceName;

    @Column(name = "commune_code", nullable = false, length = 20)
    private String communeCode;

    @Column(name = "commune_name", nullable = false, length = 100)
    private String communeName;

    @Column(name = "district_name", length = 100)
    private String districtName;

    @Column(name = "full_address", nullable = false, length = 400)
    private String fullAddress;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "lat", precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "lng", precision = 10, scale = 7)
    private BigDecimal lng;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    @Builder.Default
    private AddressSource source = AddressSource.GEOCODE_ONLY;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (source == null) {
            source = AddressSource.GEOCODE_ONLY;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}



