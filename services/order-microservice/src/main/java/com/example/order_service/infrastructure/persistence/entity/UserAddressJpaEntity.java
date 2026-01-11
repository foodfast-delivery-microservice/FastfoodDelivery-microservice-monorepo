package com.example.order_service.infrastructure.persistence.entity;

import com.example.order_service.domain.valueobjects.AddressSource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for UserAddress persistence.
 * This is the infrastructure layer representation with JPA annotations.
 * Represents a persisted address that has been normalized and optionally
 * adjusted on the map.
 */
@Entity
@Table(name = "user_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressJpaEntity {

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
    private AddressSource source = AddressSource.GEOCODE_ONLY;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Set creation and update timestamps before persist
     */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (source == null) {
            source = AddressSource.GEOCODE_ONLY;
        }
    }

    /**
     * Set update timestamp before update
     */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
